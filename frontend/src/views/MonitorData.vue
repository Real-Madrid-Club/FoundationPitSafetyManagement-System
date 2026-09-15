<template>
<div class="monitor-data">
  <PageHero title="监测数据" description="多维度监测数据查询与分析，掌握基坑结构安全状态" />
  <el-tabs v-model="activeTab" @tab-change="onTabChange">
    <el-tab-pane label="全站仪数据" name="totalStation"></el-tab-pane>
    <el-tab-pane label="伺服轴力数据" name="axialForce"></el-tab-pane>
    <el-tab-pane label="钢支撑温度" name="steelTemperature"></el-tab-pane>
  </el-tabs>
  <div class="filter-bar">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-select v-model="currentSensor" placeholder="传感器" @change="onTimeChange">
          <el-option v-for="s in sensors" :key="s" :label="s" :value="s" />
        </el-select>
      </el-col>
      <el-col :span="8">
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm:ss"
          @change="onTimeChange"
        />
      </el-col>
      <el-col :span="4">
        <el-select v-model="pagination.pageSize" placeholder="每页条数" @change="onTimeChange">
          <el-option label="50条" :value="50" />
          <el-option label="100条" :value="100" />
          <el-option label="200条" :value="200" />
        </el-select>
      </el-col>
      <el-col :span="2">
        <el-button type="warning" @click="handlePredict" :loading="predicting" :disabled="!currentSensor">
          <el-icon>
            <Histogram />
          </el-icon>
          智能预测
        </el-button>
      </el-col>
    </el-row>
  </div>
  <div class="chart-container" ref="chartPanelRef" v-loading="loading">
    <div class="section-heading">
      <h2>{{ getYLabel() }}</h2>
      <div class="chart-tools">
        <el-radio-group v-model="selectedMetric" size="small" @change="initChart(tableData)">
          <el-radio-button v-for="metric in metricOptions" :key="metric.key" :value="metric.key">{{ metric.label }}</el-radio-button>
        </el-radio-group>
        <el-button :icon="FullScreen" aria-label="全屏查看图表" @click="toggleFullscreen" />
      </div>
    </div>
    <div ref="chartRef" class="monitor-chart"></div>
    <div v-if="!loading && !tableData.length" class="chart-no-data">{{ loadError ? '数据加载失败，请重新查询' : '暂无监测数据，请选择传感器和时间范围' }}</div>
  </div>
  <div class="table-container">
    <div class="section-heading">
      <h2>数据列表</h2>
      <el-button :icon="Download" size="small" :disabled="!tableData.length" @click="exportData">导出当前页</el-button>
    </div>
    <el-table :data="tableData" border stripe v-loading="loading" max-height="400">
      <template #empty>
        <EmptyState compact :title="loadError ? '数据加载失败' : '暂无监测数据'" description="请选择传感器和时间范围后查询" />
      </template>
      <el-table-column
        prop="collectTime"
        label="采集时间"
        width="190"
        :formatter="(_row,_column,value) => value ? String(value).replace('T',' ') : '—'"
      />
      <el-table-column
        v-if="activeTab === 'totalStation'"
        prop="deltaX"
        :formatter="formatNumberCell"
        label="ΔX(mm)"
        min-width="100"
      />
      <el-table-column
        v-if="activeTab === 'totalStation'"
        prop="deltaY"
        :formatter="formatNumberCell"
        label="ΔY(mm)"
        min-width="100"
      />
      <el-table-column
        v-if="activeTab === 'totalStation'"
        prop="deltaH"
        :formatter="formatNumberCell"
        label="ΔH(mm)"
        min-width="100"
      />
      <el-table-column
        v-if="activeTab === 'totalStation'"
        prop="totalX"
        :formatter="formatNumberCell"
        label="∑X(mm)"
        min-width="100"
      />
      <el-table-column
        v-if="activeTab === 'totalStation'"
        prop="totalY"
        :formatter="formatNumberCell"
        label="∑Y(mm)"
        min-width="100"
      />
      <el-table-column
        v-if="activeTab === 'totalStation'"
        prop="totalH"
        :formatter="formatNumberCell"
        label="∑H(mm)"
        min-width="100"
      />
      <el-table-column v-if="activeTab === 'axialForce'" label="轴力(kN)" min-width="120">
        <template #default="scope">{{ formatNumber(safeGetField(scope.row, 'wForce')) }}</template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'axialForce'" label="行程(mm)" min-width="120">
        <template #default="scope">{{ formatNumber(safeGetField(scope.row, 'fPosition')) }}</template>
      </el-table-column>
      <el-table-column
        v-if="activeTab === 'steelTemperature'"
        prop="temperature"
        :formatter="formatNumberCell"
        label="温度(℃)"
        min-width="120"
      />
      <el-table-column
        v-if="activeTab === 'steelTemperature'"
        prop="measureVal"
        :formatter="formatNumberCell"
        label="原始测量值"
        min-width="120"
      />
      <el-table-column
        prop="temperature"
        :formatter="formatNumberCell"
        label="温度(℃)"
        min-width="100"
        v-if="activeTab !== 'steelTemperature'"
      />
    </el-table>
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
  <el-dialog v-model="showPredict" title="AI 智能预测" width="700px" :close-on-click-modal="false">
    <div class="predict-info">
      <el-tag>{{ currentSensor }}</el-tag>
      <span style="margin: 0 8px; color: #909399">{{ predictTimeRange }}</span>
    </div>
    <div v-if="predicting" class="predict-loading">
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <span>AI 正在分析数据趋势，请稍候...</span>
    </div>
    <div v-else-if="predictResult" class="predict-result" v-html="formatMarkdown(predictResult)" />
    <template #footer>
      <el-button @click="showPredict = false">关闭</el-button>
    </template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { Loading, Histogram, FullScreen, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  getTotalStation,
  getAxialForce,
  getSteelTemperature,
  getSensors
} from '@/api/monitor'
import { predictAuto } from '@/api/ai'

const chartRef = ref(null)
const chartPanelRef = ref(null)
const loadError = ref(false)
const formatNumber = value => value == null ? '—' : Number.isFinite(Number(value)) ? Number(Number(value).toFixed(3)) : value
const formatNumberCell = (_row,_column,value) => formatNumber(value)
let chartObserver
let requestVersion = 0
const selectedMetric = ref('totalX')
const metricOptions = computed(() => ({
 totalStation: [{key:'totalX',label:'∑X(mm)'},{key:'totalY',label:'∑Y(mm)'},{key:'totalH',label:'∑H(mm)'}],
 axialForce: [{key:'wForce',label:'轴力(kN)'},{key:'fPosition',label:'行程(mm)'},{key:'temperature',label:'温度(℃)'}],
 steelTemperature: [{key:'temperature',label:'温度(℃)'},{key:'measureVal',label:'原始测量值'}]
}[activeTab.value]))
async function toggleFullscreen(){try {if(document.fullscreenElement) await document.exitFullscreen();else await chartPanelRef.value?.requestFullscreen()}catch{ElMessage.info('当前浏览器不支持全屏')}}
function exportData(){
 const columns = [{key:'collectTime',label:'采集时间'},...(activeTab.value==='totalStation'?[{key:'deltaX',label:'ΔX(mm)'},{key:'deltaY',label:'ΔY(mm)'},{key:'deltaH',label:'ΔH(mm)'},{key:'totalX',label:'∑X(mm)'},{key:'totalY',label:'∑Y(mm)'},{key:'totalH',label:'∑H(mm)'}]:activeTab.value==='axialForce'?[{key:'wForce',label:'轴力(kN)'},{key:'fPosition',label:'行程(mm)'}]:[{key:'measureVal',label:'原始测量值'}]),{key:'temperature',label:'温度(℃)'}]
 const quote=value=>{let text=String(value??'');if(/^[=+@\t\r]/.test(text)||(/^[-]/.test(text)&&!Number.isFinite(Number(text))))text="'"+text;return '"'+text.replace(/"/g,'""')+'"'}
 const csv=[columns.map(x=>quote(x.label)).join(','),...tableData.value.map(row=>columns.map(x=>quote(safeGetField(row,x.key))).join(','))].join('\r\n')
 const url=URL.createObjectURL(new Blob(['\ufeff'+csv],{type:'text/csv;charset=utf-8;'}));const a=document.createElement('a');a.href=url;a.download=currentSensor.value+'-监测数据-第'+pagination.currentPage+'页.csv';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)
}
let chartInstance = null
const loading = ref(false)
const activeTab = ref('totalStation')
const currentSensor = ref('')
const timeRange = ref(null)
const defaultStartTime = '2025-01-01 00:00:00'
const defaultEndTime = '2026-12-31 23:59:59'

const sensors = ref([])
const allSensors = ref([])
const tableData = ref([])

const pagination = reactive({
  currentPage: 1,
  pageSize: 100,
  total: 0
})

const showPredict = ref(false)
const predicting = ref(false)
const predictResult = ref('')

const predictTimeRange = computed(() => {
  if (timeRange.value && timeRange.value.length === 2) {
    return `${timeRange.value[0]} ~ ${timeRange.value[1]}`
  }
  return `${defaultStartTime} ~ ${defaultEndTime}`
})

const getApiMethod = () => {
  const apiMap = {
    totalStation: getTotalStation,
    axialForce: getAxialForce,
    steelTemperature: getSteelTemperature
  }
  return apiMap[activeTab.value]
}

const getYField = () => selectedMetric.value

const safeGetField = (item, field) => {
  return item[field] !== undefined ? item[field] : item[field.toLowerCase()]
}

const getYLabel = () => ({totalX:'累计X位移 ∑X(mm)',totalY:'累计Y位移 ∑Y(mm)',totalH:'累计H位移 ∑H(mm)',wForce:'轴力 WForce(kN)',fPosition:'行程(mm)',temperature:'温度(℃)',measureVal:'原始测量值'}[selectedMetric.value])

const filterSensorsByType = (sensorList, type) => {
  if (!sensorList || !Array.isArray(sensorList)) return []
  return sensorList.filter(s => {
    if (type === 'totalStation') {
      return s.startsWith('FRHY')
    } else if (type === 'axialForce') {
      return s.startsWith('SP') || s.startsWith('4P') || s.startsWith('HSD')
    } else if (type === 'steelTemperature') {
      return s.startsWith('650')
    }
    return true
  })
}

const initChart = (data) => {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const yField = getYField()
  const ordered = [...data].sort((a,b) => String(a.collectTime).localeCompare(String(b.collectTime)))
  const times = ordered.map(item => String(item.collectTime).replace('T',' '))
  const values = ordered.map(item => safeGetField(item, yField) ?? null)

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 20, top: 18, bottom: 55 },
    xAxis: {
      type: 'category',
      data: times,
      boundaryGap: false,
      axisLabel: { color: '#7186aa', fontSize: 11, formatter: value => value.slice(5,16) },
      axisLine: { lineStyle: { color: '#dce6f8' } }, axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#7186aa' }, splitLine: { lineStyle: { type: 'dashed', color: '#deebff' } }
    },
    dataZoom: [
      { type: 'inside', start: 0, end: 100 },
      { type: 'slider', start: 0, end: 100, height: 20, bottom: 0 }
    ],
    series: [{
      data: values,
      type: 'line',
      smooth: false,
      showSymbol: false,
      lineStyle: { width: 2.5 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#2385ff45'},{offset:1,color:'#2385ff03'}]) },
      itemStyle: { color: '#267fff' }
    }]
  })
}

const loadData = async () => {
  const version = ++requestVersion
  if (!currentSensor.value) { tableData.value=[]; pagination.total=0; loading.value=false; await nextTick(); initChart([]); return }
  loading.value = true
  loadError.value = false
  try {
    const params = {
      sensorCode: currentSensor.value,
      startTime: (timeRange.value && timeRange.value.length === 2) ? timeRange.value[0] : defaultStartTime,
      endTime: (timeRange.value && timeRange.value.length === 2) ? timeRange.value[1] : defaultEndTime,
      page: pagination.currentPage,
      pageSize: pagination.pageSize
    }
    const apiMethod = getApiMethod()
    const data = await apiMethod(params)
    if (version !== requestVersion) return

    if (data && data.list) {
      tableData.value = data.list
      pagination.total = data.total || 0
    } else if (Array.isArray(data)) {
      tableData.value = data
      pagination.total = data.length
    }

    await nextTick()
    initChart(tableData.value)
  } catch (error) {
    if (version !== requestVersion) return
    loadError.value = true
    tableData.value = []
    pagination.total = 0
    initChart([])
    console.error('加载监测数据失败:', error)
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

const onTabChange = () => {
  ++requestVersion
  loading.value = false
  selectedMetric.value = metricOptions.value[0].key
  pagination.total = 0
  initChart([])
  currentSensor.value = ''
  tableData.value = []
  pagination.currentPage = 1
  sensors.value = filterSensorsByType(allSensors.value, activeTab.value)
  if (sensors.value.length > 0) {
    currentSensor.value = sensors.value[0]
    loadData()
  }
}

const onTimeChange = () => {
  pagination.currentPage = 1
  loadData()
}

const loadSensorList = async () => {
  try {
    const data = await getSensors()
    allSensors.value = data || []
    sensors.value = filterSensorsByType(allSensors.value, activeTab.value)
    if (sensors.value.length > 0) {
      currentSensor.value = sensors.value[0]
      loadData()
    }
  } catch (error) {
    loadError.value = true
    console.error('加载传感器列表失败:', error)
  }
}

const handlePredict = async () => {
  if (!currentSensor.value) return
  showPredict.value = true
  predicting.value = true
  predictResult.value = ''
  try {
    const startTime = (timeRange.value && timeRange.value.length === 2) ? timeRange.value[0] : defaultStartTime
    const endTime = (timeRange.value && timeRange.value.length === 2) ? timeRange.value[1] : defaultEndTime
    const result = await predictAuto({
      sensorCode: currentSensor.value,
      startTime,
      endTime
    })
    predictResult.value = result || 'AI 未返回预测结果'
  } catch (e) {
    predictResult.value = '预测请求失败，请检查后端服务和 API Key 配置。'
  } finally {
    predicting.value = false
  }
}

const formatMarkdown = (text) => {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/### (.*)/g, '<h4>$1</h4>')
    .replace(/## (.*)/g, '<h3>$1</h3>')
    .replace(/# (.*)/g, '<h2>$1</h2>')
    .replace(/\*\*(.*?)\*\*/g, '<b>$1</b>')
    .replace(/\*(.*?)\*/g, '<i>$1</i>')
    .replace(/- (.*)/g, '<li>$1</li>')
    .replace(/\n\n/g, '<br/><br/>')
    .replace(/\n/g, '<br/>')
}

onMounted(() => {
  loadSensorList()
  initChart([])
  chartObserver = new ResizeObserver(() => chartInstance?.resize())
  chartObserver.observe(chartRef.value)
})
onBeforeUnmount(() => { ++requestVersion; chartObserver?.disconnect(); chartInstance?.dispose(); chartInstance = null })
</script>

<style scoped>
.monitor-data>.el-tabs {
  background: #ffffffbc;
  border-radius: 13px 13px 0 0;
  padding: 0 20px;
}

.monitor-data :deep(.el-tabs__header) {
  margin: 0;
}

.monitor-data :deep(.el-tabs__nav-wrap:after) {
  display: none;
}

.monitor-data :deep(.el-tabs__item) {
  height: 47px;
  font-size: 15px;
  font-weight: 600;
  padding: 0 24px;
}

.monitor-data :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px;
}

.monitor-data .filter-bar {
  padding: 20px;
  margin-top: 0;
}

.monitor-data .filter-bar :deep(.el-row) {
  display: grid;
  grid-template-columns: minmax(140px,1fr) minmax(310px,2fr) minmax(105px,.7fr) auto;
  margin: 0 !important;
  gap: 14px;
}

.monitor-data .filter-bar :deep(.el-col) {
  max-width: none;
  width: auto;
  padding: 0 !important;
  min-width: 0;
}

.monitor-data .filter-bar :deep(.el-date-editor) {
  width: 100%;
  box-sizing: border-box;
  min-width: 0;
}

.monitor-data .filter-bar :deep(.el-range-input) {
  font-size: 12px;
}

.monitor-data .filter-bar .el-button {
  padding: 0 18px;
}

.chart-container {
  position: relative;
  padding: 20px 20px 12px;
  margin-bottom: 16px;
}

.monitor-chart {
  height: 280px;
  width: 100%;
}

.chart-tools {
  display: flex;
  align-items: center;
  gap: 14px;
}

.chart-tools>.el-button {
  padding: 8px;
  font-size: 18px;
  height: 30px;
}

.chart-no-data {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  text-align: center;
  font-size: 14px;
  color: #93a2bd;
  pointer-events: none;
}

.chart-container:fullscreen {
  background: #fff;
  padding: 30px;
  display: flex;
  flex-direction: column;
}

.chart-container:fullscreen .monitor-chart {
  flex: 1;
  height: auto;
}

.chart-tools :deep(.el-radio-button__inner) {
  background: #f4f7ff;
  border-color: #e1ebfc;
  color: #5f76a0;
}

.chart-tools :deep(.el-radio-button__original-radio:checked+.el-radio-button__inner) {
  background: #347cff;
  color: white;
  border-color: #347cff;
}

.monitor-data .table-container {
  padding: 16px;
}

.monitor-data .table-container :deep(.el-table td.el-table__cell) {
  height: 38px;
}

.monitor-data .table-container :deep(.el-table th.el-table__cell) {
  height: 40px;
}

.monitor-data .table-container .el-button {
  color: #6880ac;
  background: #f6f9ff;
}

.predict-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 15px;
}

.predict-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px;
  color: #6988b7;
}

.predict-loading .el-icon {
  font-size: 30px;
}

.predict-result {
  max-height: 60vh;
  overflow: auto;
  padding: 20px;
  background: #f6f9ff;
  border: 1px solid #e4edfb;
  border-radius: 10px;
  line-height: 1.9;
  overflow-wrap: anywhere;
}

.predict-result :deep(h2),.predict-result :deep(h3),.predict-result :deep(h4) {
  color: #274e88;
  margin: 14px 0 6px;
}

.predict-result :deep(li) {
  margin-left: 18px;
}

@media (min-width:1600px) {
  .monitor-chart {
    height: 290px;
  }
}

@media (max-width:1200px) {
  .monitor-data .filter-bar :deep(.el-row) {
    grid-template-columns: minmax(140px,1fr) minmax(280px,2fr);
  }
  .chart-tools {
    gap: 8px;
  }
  .section-heading h2 {
    font-size: 15px;
  }
}

@media (max-width:700px) {
  .monitor-data .filter-bar :deep(.el-row) {
    grid-template-columns: 1fr;
  }
  .monitor-data>.el-tabs {
    padding: 0 10px;
  }
  .monitor-data :deep(.el-tabs__item) {
    padding: 0 12px;
    font-size: 12px;
  }
  .chart-container {
    padding: 14px 10px;
  }
  .chart-tools {
    width: 100%;
    justify-content: space-between;
  }
  .monitor-chart {
    height: 260px;
  }
  .monitor-data .table-container {
    padding: 12px;
  }
}
</style>
