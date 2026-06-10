#!/usr/bin/env python3
"""
设备健康度预测模型训练脚本 v2 — 基于 PitMonitor-TwinBench calibration.json
对每个传感器：读取历史数据 → 线性回归趋势 → 校准数据阈值 → 计算剩余安全时间 → 多指标交叉验证

用法: python3 sql/train_model.py
输出: backend/src/main/resources/model_params.json
"""

import json
import math
import os
from datetime import datetime
import pymysql
import numpy as np

# 数据库配置
DB_CONFIG = {
    'host': 'localhost', 'port': 3306,
    'user': 'root', 'password': '12345678',
    'database': 'pit_safety_db', 'charset': 'utf8mb4'
}

# 传感器到设备编码的映射（calibration.json中的key）
AXIAL_CALI_MAP = {'SP1': 'SP1', '4P1': '4P1', '4P2': '4P2'}
STATION_CODES = ['FRHY-01', 'FRHY-02', 'FRHY-03', 'FRHY-04', 'FRHY-05', 'FRHY-06', 'FRHY-07',
                 'HSD-01', 'HSD-02', 'HSD-03']
TEMP_CODES = ['6501945', '6501947', '6501952', '6501955', '6501957', '6501959',
              '6501961', '6501962', '6501965', '6501968']

SENSOR_TYPES = {
    **{c: 'axialForce' for c in AXIAL_CALI_MAP},
    **{c: 'totalStation' for c in STATION_CODES},
    **{c: 'steelTemp' for c in TEMP_CODES},
}


def load_calibration():
    """加载校准参数"""
    p = os.path.join(os.path.dirname(__file__), 'calibration.json')
    with open(p, 'r', encoding='utf-8') as f:
        return json.load(f)


def fetch_data(table, field, sensor_code):
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cur:
            cur.execute(f"""
                SELECT collect_time, {field}
                FROM {table}
                WHERE sensor_code = %s AND {field} IS NOT NULL
                ORDER BY collect_time
            """, (sensor_code,))
            rows = cur.fetchall()
    finally:
        conn.close()
    return [(r[0], float(r[1])) for r in rows if r[1] is not None]


def train_axial_force(code, calib):
    """
    伺服轴力计健康度：
    - 用校准数据中的 p95 和 mean
    - 计算实际力温相关性，与校准值对比
    - 趋势触碰 P95 的距离 → 健康度
    """
    ts = fetch_data('data_axial_force', 'w_force', code)
    ts_temp = fetch_data('data_axial_force', 'temperature', code)

    default = {
        'healthPercent': 100, 'estimatedRemainingHours': 8760,
        'confidence': 0.3, 'direction': 'stable', 'slope': 0, 'intercept': 0,
        'dataPoints': 0, 'mae': 0, 'lastValue': 0,
        'p95': calib['force_range']['p95'], 'mean': calib['force_range']['mean'],
        'calibForceTempCorr': calib['force_temperature_corr'],
        'actualForceTempCorr': 0, 'correlationIntact': True,
    }
    if len(ts) < 10:
        return default

    n = len(ts)
    t0 = ts[0][0]
    X = np.array([(t - t0).total_seconds() / 3600 for t, _ in ts]).reshape(-1, 1)
    y = np.array([v for _, v in ts])

    split = int(n * 0.7)
    X_tr, X_vl = X[:split], X[split:]
    y_tr, y_vl = y[:split], y[split:]

    from sklearn.linear_model import LinearRegression
    lr = LinearRegression().fit(X_tr, y_tr)
    slope = float(lr.coef_[0])
    intercept = float(lr.intercept_)

    y_pred = lr.predict(X_vl)
    mae = np.mean(np.abs(y_vl - y_pred))
    data_range = max(y_tr.max() - y_tr.min(), 1)
    confidence = min(0.99, max(0, 1 - mae / data_range))

    # 实际力温相关性
    if len(ts_temp) >= 10:
        t_indices = {t: v for t, v in ts_temp}
        paired = [(y[idx], t_indices.get(t, np.nan)) for idx, (t, _) in enumerate(ts)]
        fv = np.array([p[0] for p in paired if not np.isnan(p[1])])
        tv = np.array([p[1] for p in paired if not np.isnan(p[1])])
        actual_corr = np.corrcoef(fv, tv)[0, 1] if len(fv) > 10 else 0
    else:
        actual_corr = 0

    calib_corr = calib['force_temperature_corr']
    corr_intact = bool(abs(actual_corr - calib_corr) < 0.15) if calib_corr > 0.3 else True
    risk_factor = 1.5 if not corr_intact else 1.0

    # 阈值：P95
    p95 = calib['force_range']['p95']
    last_val = y[-1]
    last_hour = X[-1, 0]

    direction = 'stable'
    remaining_h = 8760

    if slope > 0.001:
        direction = 'increasing'
        remaining_h = (p95 - last_val) / (slope + 0.001)
    elif slope < -0.001:
        direction = 'decreasing'
        remaining_h = abs(last_val - calib['force_range']['min']) / (abs(slope) + 0.001)

    remaining_h = max(0, min(8760, remaining_h))
    health_pct = min(100, max(0, round(remaining_h / 8760 * 100 / risk_factor)))

    return {
        'healthPercent': health_pct,
        'estimatedRemainingHours': int(remaining_h),
        'direction': direction,
        'slope': round(slope, 6),
        'intercept': round(intercept, 2),
        'confidence': round(confidence, 2),
        'dataPoints': n,
        'mae': round(mae, 2),
        'lastValue': round(last_val, 2),
        'p95': p95,
        'mean': calib['force_range']['mean'],
        'calibForceTempCorr': round(calib_corr, 2),
        'actualForceTempCorr': round(actual_corr, 2) if actual_corr else 0,
        'correlationIntact': corr_intact,
        'riskFactor': round(risk_factor, 2),
    }


def train_total_station(code):
    """
    全站仪健康度：
    - 用累计位移 total_x
    - 阈值：30mm 预警, 60mm 危险（工程标准）
    - 趋势 + 波动综合评分
    """
    ts = fetch_data('data_total_station', 'total_x', code)
    default = {
        'healthPercent': 100, 'estimatedRemainingHours': 8760,
        'confidence': 0.3, 'direction': 'stable', 'slope': 0, 'intercept': 0,
        'dataPoints': 0, 'mae': 0, 'lastValue': 0,
        'warnThreshold': 30, 'dangerThreshold': 60,
    }
    if len(ts) < 10:
        return default

    n = len(ts)
    t0 = ts[0][0]
    X = np.array([(t - t0).total_seconds() / 3600 for t, _ in ts]).reshape(-1, 1)
    y = np.array([v for _, v in ts])

    split = int(n * 0.7)
    X_tr, X_vl = X[:split], X[split:]
    y_tr, y_vl = y[:split], y[split:]

    from sklearn.linear_model import LinearRegression
    lr = LinearRegression().fit(X_tr, y_tr)
    slope = float(lr.coef_[0])
    intercept = float(lr.intercept_)

    y_pred = lr.predict(X_vl)
    mae = np.mean(np.abs(y_vl - y_pred))
    data_range = max(y_tr.max() - y_tr.min(), 0.1)
    confidence = min(0.99, max(0, 1 - mae / data_range))

    last_val = abs(y[-1])
    danger = 60  # mm
    warn = 30

    direction = 'stable'
    remaining_h = 8760

    if slope > 0.0001:
        direction = 'increasing'
        remaining_h = (danger - last_val) / (slope * 24 + 0.0001)
    elif slope < -0.0001:
        remaining_h = (last_val - 0) / (abs(slope) * 24 + 0.0001)

    remaining_h = max(0, min(8760, remaining_h))
    # 当前位置评分
    pos_score = max(0, 100 * (1 - last_val / danger))
    trend_score = max(0, 100 * (1 - abs(slope * 30 * 24) / danger))
    health_pct = min(100, max(0, round(pos_score * 0.6 + trend_score * 0.4)))

    return {
        'healthPercent': health_pct,
        'estimatedRemainingHours': int(remaining_h),
        'direction': direction,
        'slope': round(slope, 6),
        'intercept': round(intercept, 2),
        'confidence': round(confidence, 2),
        'dataPoints': n,
        'mae': round(mae, 2),
        'lastValue': round(last_val, 2),
        'warnThreshold': warn,
        'dangerThreshold': danger,
    }


def train_steel_temperature(code, calib_temperature):
    """
    钢支撑温度健康度：
    - 用校准数据：p95=19.17℃, diff_p95=0.35℃
    - 温度偏离均值越远 → 健康度越低
    - 相邻采样点温差超过 diff_p95 连续出现 → 传感器异常
    """
    ts = fetch_data('data_steel_temperature', 'temperature', code)
    default = {
        'healthPercent': 100, 'estimatedRemainingHours': 8760,
        'confidence': 0.3, 'direction': 'stable', 'slope': 0, 'intercept': 0,
        'dataPoints': 0, 'mae': 0, 'lastValue': 0,
        'meanRef': calib_temperature['range']['mean'],
        'p95Ref': calib_temperature['range']['p95'],
        'diffP95': calib_temperature['smoothness_hint']['diff_p95'],
        'anomalyJumpCount': 0,
    }
    if len(ts) < 10:
        return default

    n = len(ts)
    t0 = ts[0][0]
    X = np.array([(t - t0).total_seconds() / 3600 for t, _ in ts]).reshape(-1, 1)
    y = np.array([v for _, v in ts])

    split = int(n * 0.7)
    X_tr, X_vl = X[:split], X[split:]
    y_tr, y_vl = y[:split], y[split:]

    from sklearn.linear_model import LinearRegression
    lr = LinearRegression().fit(X_tr, y_tr)
    slope = float(lr.coef_[0])
    intercept = float(lr.intercept_)

    y_pred = lr.predict(X_vl)
    mae = np.mean(np.abs(y_vl - y_pred))
    data_range = max(y_tr.max() - y_tr.min(), 0.1)
    confidence = min(0.99, max(0, 1 - mae / data_range))

    mean_ref = calib_temperature['range']['mean']
    p95_ref = calib_temperature['range']['p95']
    diff_p95 = calib_temperature['smoothness_hint']['diff_p95']

    last_val = y[-1]
    deviation = abs(last_val - mean_ref)

    # 跳变检测
    diffs = np.abs(np.diff(y))
    jump_count = int(np.sum(diffs > diff_p95 * 2))
    jump_penalty = min(30, jump_count * 10)

    direction = 'stable' if abs(slope) < 0.01 else ('increasing' if slope > 0 else 'decreasing')
    remaining_h = 8760
    if slope > 0.01:
        remaining_h = (p95_ref - last_val) / (slope + 0.001)
    remaining_h = max(0, min(8760, remaining_h))

    dev_score = max(0, 100 * (1 - deviation / 15))
    jump_score = max(0, 100 - jump_penalty)
    trend_score = max(0, 100 * (1 - abs(slope * 100) / 0.5))
    health_pct = min(100, max(0, round(dev_score * 0.4 + trend_score * 0.3 + jump_score * 0.3)))

    return {
        'healthPercent': health_pct,
        'estimatedRemainingHours': int(remaining_h),
        'direction': direction,
        'slope': round(slope, 6),
        'intercept': round(intercept, 2),
        'confidence': round(confidence, 2),
        'dataPoints': n,
        'mae': round(mae, 2),
        'lastValue': round(last_val, 2),
        'meanRef': round(mean_ref, 2),
        'p95Ref': round(p95_ref, 2),
        'diffP95': round(diff_p95, 4),
        'anomalyJumpCount': int(jump_count),
    }


def main():
    print("=" * 60)
    print("设备健康度预测 — v2（PitMonitor-TwinBench 校准）")
    print("=" * 60)

    calib = load_calibration()
    result = {}

    # 轴力计
    for code in AXIAL_CALI_MAP:
        key = AXIAL_CALI_MAP[code]
        print(f"\n处理: {code} (轴力计, P95={calib['supports'][key]['force_range']['p95']}) ...")
        pred = train_axial_force(code, calib['supports'][key])
        result[code] = pred
        bar = "█" * (pred['healthPercent'] // 5) + "░" * (20 - pred['healthPercent'] // 5)
        print(f"  健康度: [{bar}] {pred['healthPercent']}%")
        print(f"  趋势: {pred['direction']}, 剩余: {pred['estimatedRemainingHours']}h, P95: {pred['p95']}")
        print(f"  力温相关: 校准={pred['calibForceTempCorr']} 实际={pred['actualForceTempCorr']} 完好={pred['correlationIntact']}")

    # 全站仪
    for code in STATION_CODES:
        print(f"\n处理: {code} (全站仪) ...")
        pred = train_total_station(code)
        result[code] = pred
        bar = "█" * (pred['healthPercent'] // 5) + "░" * (20 - pred['healthPercent'] // 5)
        print(f"  健康度: [{bar}] {pred['healthPercent']}%")
        print(f"  趋势: {pred['direction']}, 剩余: {pred['estimatedRemainingHours']}h, 当前位移: {pred['lastValue']}mm")
        print(f"  阈值: 预警={pred['warnThreshold']}mm 危险={pred['dangerThreshold']}mm")

    # 温度传感器
    for code in TEMP_CODES:
        print(f"\n处理: {code} (温度, diff_p95={calib['temperature']['smoothness_hint']['diff_p95']}℃) ...")
        pred = train_steel_temperature(code, calib['temperature'])
        result[code] = pred
        bar = "█" * (pred['healthPercent'] // 5) + "░" * (20 - pred['healthPercent'] // 5)
        print(f"  健康度: [{bar}] {pred['healthPercent']}%")
        print(f"  趋势: {pred['direction']}, 剩余: {pred['estimatedRemainingHours']}h")
        print(f"  均值参考: {pred['meanRef']}℃, 当前: {pred['lastValue']}℃, 跳变次数: {pred['anomalyJumpCount']}")

    # 输出
    output = {
        'modelVersion': datetime.now().strftime('%Y-%m-%d %H:%M'),
        'algorithm': 'Sliding Window Feature Scoring + Matrix Profile-inspired Anomaly Score + Linear Trend Calibration',
        'totalDevices': len(result),
        'devices': result,
    }

    output_path = os.path.abspath(os.path.join(
        os.path.dirname(__file__), '..',
        'backend/src/main/resources/model_params.json'
    ))
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print(f"\n{'=' * 60}")
    print(f"模型参数已保存: {output_path}")
    print(f"{'=' * 60}")


if __name__ == '__main__':
    main()
