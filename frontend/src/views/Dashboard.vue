<template>
  <div class="dashboard">
    <div class="page-header">
      <h2>首页大盘</h2>
    </div>

    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-content">
              <div class="stats-icon normal">
                <el-icon><Monitor /></el-icon>
              </div>
              <div class="stats-info">
                <h3>156</h3>
                <p>设备总数</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-content">
              <div class="stats-icon success">
                <el-icon><CircleCheck /></el-icon>
              </div>
              <div class="stats-info">
                <h3>142</h3>
                <p>正常运行</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-content">
              <div class="stats-icon warning">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="stats-info">
                <h3>8</h3>
                <p>需要维护</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-content">
              <div class="stats-icon danger">
                <el-icon><CircleClose /></el-icon>
              </div>
              <div class="stats-info">
                <h3>6</h3>
                <p>设备故障</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="charts-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>设备状态分布</span>
              </div>
            </template>
            <div class="chart-placeholder">
              <el-empty description="图表区域" />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>近7天告警趋势</span>
              </div>
            </template>
            <div class="chart-placeholder">
              <el-empty description="图表区域" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="recent-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>最近告警</span>
                <el-button type="text">查看全部</el-button>
              </div>
            </template>
            <div class="alert-list">
              <div class="alert-item" v-for="alert in recentAlerts" :key="alert.id">
                <div class="alert-info">
                  <span class="alert-title">{{ alert.title }}</span>
                  <span class="alert-time">{{ alert.time }}</span>
                </div>
                <el-tag :type="alert.type" size="small">{{ alert.level }}</el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>待处理工单</span>
                <el-button type="text">查看全部</el-button>
              </div>
            </template>
            <div class="workorder-list">
              <div class="workorder-item" v-for="order in recentWorkOrders" :key="order.id">
                <div class="workorder-info">
                  <span class="workorder-title">{{ order.title }}</span>
                  <span class="workorder-time">{{ order.time }}</span>
                </div>
                <el-tag :type="order.priority" size="small">{{ order.priorityText }}</el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { Monitor, CircleCheck, Warning, CircleClose } from '@element-plus/icons-vue'

const recentAlerts = [
  { id: 1, title: '测斜仪-01号数据异常', time: '2小时前', level: '高', type: 'danger' },
  { id: 2, title: '水位计-03号离线', time: '3小时前', level: '中', type: 'warning' },
  { id: 3, title: '土压力计-02号需要校准', time: '5小时前', level: '低', type: 'info' }
]

const recentWorkOrders = [
  { id: 1, title: '测斜仪-01号检修', time: '1小时前', priority: 'danger', priorityText: '紧急' },
  { id: 2, title: '水位计-03号故障排查', time: '2小时前', priority: 'warning', priorityText: '一般' },
  { id: 3, title: '土压力计-02号定期维护', time: '4小时前', priority: 'info', priorityText: '低优先级' }
]
</script>

<style scoped>
.dashboard {
  background: white;
  border-radius: 8px;
  padding: 20px;
}

.page-header h2 {
  margin: 0;
  color: #2c3e50;
}

.stats-cards {
  margin-bottom: 20px;
}

.stats-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stats-card:hover {
  transform: translateY(-2px);
}

.stats-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stats-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stats-icon.normal {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stats-icon.success {
  background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%);
}

.stats-icon.warning {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stats-icon.danger {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stats-info h3 {
  margin: 0;
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
}

.stats-info p {
  margin: 5px 0 0 0;
  color: #7f8c8d;
  font-size: 14px;
}

.charts-section {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.alert-list, .workorder-list {
  max-height: 300px;
  overflow-y: auto;
}

.alert-item, .workorder-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.alert-item:last-child, .workorder-item:last-child {
  border-bottom: none;
}

.alert-info, .workorder-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.alert-title, .workorder-title {
  font-weight: 500;
  color: #2c3e50;
}

.alert-time, .workorder-time {
  font-size: 12px;
  color: #7f8c8d;
}
</style>
