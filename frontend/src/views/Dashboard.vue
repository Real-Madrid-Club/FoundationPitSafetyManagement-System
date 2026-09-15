<template>
<div class="dashboard">
  <PageHero title="首页大盘" description="掌握基坑安全运行状态 · 数据驱动安全管理" />
  <el-alert
    v-if="loadError"
    title="部分数据加载失败，请检查连接后重试"
    type="error"
    show-icon
    :closable="false"
    class="dashboard-error"
  >
    <el-button text type="primary" @click="loadData">重新加载</el-button>
  </el-alert>
  <div class="stats-grid" v-loading="loading">
    <div v-for="card in cards" :key="card.key" class="stat-card" :class="card.tone">
      <div class="stat-icon">
        <el-icon>
          <component :is="card.icon" />
        </el-icon>
      </div>
      <div class="stat-copy">
        <strong>{{ stats[card.key] ?? '—' }}</strong>
        <span>{{ card.label }}</span>
      </div>
      <svg class="stat-wave" viewBox="0 0 120 40" aria-hidden="true">
        <path d="M0 39Q25 34 47 16T86 16T120 12V40H0" fill="currentColor" opacity=".10" />
        <path d="M0 39Q25 34 47 16T86 16T120 12" fill="none" stroke="currentColor" opacity=".30" />
      </svg>
    </div>
  </div>
  <div class="dashboard-grid">
    <section class="surface chart-card">
      <div class="section-heading">
        <h2>设备状态分布</h2>
        <router-link v-if="isAdmin" to="/device" class="small-link">
          查看详情
          <el-icon>
            <Right />
          </el-icon>
        </router-link>
      </div>
      <div class="distribution">
        <div ref="pieChartRef" class="donut-chart"></div>
        <div class="status-legend">
          <div v-for="item in legend" :key="item.key">
            <span class="legend-label">
              <i :style="{background:item.color}"></i>
              {{ item.label }}
            </span>
            <b>{{ stats[item.key] ?? '—' }}</b>
            <span :style="{color:item.color}">{{ stats.total ? Math.round(stats[item.key] / stats.total * 100) + '%' : '—' }}</span>
          </div>
        </div>
      </div>
    </section>
    <section class="surface chart-card">
      <div class="section-heading">
        <h2>近7天告警趋势</h2>
        <span class="chart-period">{{ trendPeriod }}</span>
      </div>
      <div ref="lineChartRef" class="trend-chart"></div>
    </section>
    <section class="surface records-card">
      <div class="section-heading">
        <h2>
          <el-icon>
            <BellFilled />
          </el-icon>
          最近告警
        </h2>
        <router-link v-if="isAdmin" to="/device" class="small-link">
          更多
          <el-icon>
            <Right />
          </el-icon>
        </router-link>
      </div>
      <el-table :data="recentAlerts" border>
        <el-table-column prop="deviceCode" label="设备编码" min-width="100" />
        <el-table-column prop="deviceName" label="设备名称" min-width="120" />
        <el-table-column prop="deviceType" label="设备类型" min-width="100" />
        <el-table-column label="状态" width="105">
          <template #default="{row}">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState compact :title="loadError ? '数据暂不可用' : '暂无告警数据'" description="此处展示设备异常记录" />
        </template>
      </el-table>
    </section>
    <section class="surface records-card">
      <div class="section-heading">
        <h2>
          <el-icon>
            <List />
          </el-icon>
          待处理工单
        </h2>
        <router-link to="/workorder" class="small-link">
          更多
          <el-icon>
            <Right />
          </el-icon>
        </router-link>
      </div>
      <el-table :data="pendingOrders" border>
        <el-table-column prop="orderNo" label="工单编号" min-width="130" />
        <el-table-column prop="deviceCode" label="设备编码" min-width="95" />
        <el-table-column prop="faultDesc" label="故障描述" min-width="100" show-overflow-tooltip />
        <el-table-column label="状态" width="85">
          <template #default="{row}">
            <el-tag>{{ orderStatus[row.status] || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState compact :title="loadError ? '数据暂不可用' : '暂无待处理工单'" description="此处展示需要处理的工单" />
        </template>
      </el-table>
    </section>
  </div>
</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Monitor, CircleCheckFilled, Tools, WarningFilled, Right, BellFilled, List } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getStats, getAlertTrend, getRecentAlerts, getPendingOrders } from '@/api/dashboard'
const pieChartRef=ref(null),lineChartRef=ref(null),loading=ref(true),loadError=ref(false)
const stats=reactive({total:null,normal:null,warning:null,error:null,scrap:null})
const recentAlerts=ref([]),pendingOrders=ref([]),trendPeriod=ref('近 7 天')
let pieChart,lineChart,observer
const isAdmin=computed(()=>{try {const user=JSON.parse(localStorage.getItem('user')||'{}');return (user.roles||user.userInfo?.roles||[]).includes('ROLE_ADMIN')}catch{return false}})
const cards=[{key:'total',label:'设备总数',icon:Monitor,tone:'blue'},{key:'normal',label:'正常运行',icon:CircleCheckFilled,tone:'green'},{key:'warning',label:'需要维护',icon:Tools,tone:'amber'},{key:'error',label:'设备故障',icon:WarningFilled,tone:'red'}]
const legend=[{key:'normal',label:'正常运行',color:'#52bd32'},{key:'warning',label:'需要维护',color:'#ffa42a'},{key:'error',label:'设备故障',color:'#ff6473'},{key:'scrap',label:'已报废',color:'#94a0b5'}]
const statusType=s=>({1:'success',2:'warning',3:'danger',4:'info'}[s]||'info')
const statusText=s=>({1:'正常',2:'异常预警',3:'故障待修',4:'已报废'}[s]||'未知')
const orderStatus={[-1]:'已取消',0:'待派单',1:'待接单',2:'维修中',3:'待验收',4:'已完成'}
const normalize=row=>Object.fromEntries(Object.entries(row).map(([k,v])=>[k.replace(/_([a-z])/g,(_,c)=>c.toUpperCase()),v]))
function renderCharts(trend){
  if(!pieChartRef.value||!lineChartRef.value)return
  pieChart ||= echarts.init(pieChartRef.value); lineChart ||= echarts.init(lineChartRef.value)
  pieChart.setOption({color:legend.map(x=>x.color),tooltip:{trigger:'item'},title:{text:stats.total == null ? '—' : String(stats.total),subtext:'设备总数',left:'center',top:'38%',textStyle:{fontSize:29,color:'#152e5b'},subtextStyle:{fontSize:12,color:'#7284a3'}},series:[{type:'pie',radius:['61%','82%'],center:['50%','50%'],label:{show:false},data:legend.map(x=>({name:x.label,value:stats[x.key]||0})),emptyCircleStyle:{color:'#edf3fc'}}]})
  lineChart.setOption({color:['#287aff'],tooltip:{trigger:'axis'},grid:{left:36,right:16,top:20,bottom:34},xAxis:{type:'category',data:trend.map(x=>String(x.date).slice(5,10)),boundaryGap:false,axisLine:{lineStyle:{color:'#dbe6f6'}},axisTick:{show:false},axisLabel:{color:'#7b8eae',fontSize:11}},yAxis:{type:'value',minInterval:1,axisLabel:{color:'#7b8eae'},splitLine:{lineStyle:{color:'#e9eff8'}}},series:[{type:'line',data:trend.map(x=>Number(x.count)||0),symbol:'circle',symbolSize:7,lineStyle:{width:2.5},itemStyle:{borderColor:'#fff',borderWidth:2},areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#287aff35'},{offset:1,color:'#287aff02'}])}}],graphic:trend.length?[]:[{type:'text',left:'center',top:'middle',style:{text:loadError.value?'数据暂不可用':'暂无趋势数据',fill:'#8da0bc',fontSize:14}}]},true)
}
async function loadData(){
 loading.value=true;loadError.value=false
 const results=await Promise.allSettled([getStats(),getAlertTrend(),getRecentAlerts(),getPendingOrders()])
 loadError.value=results.some(x=>x.status==='rejected')
 if(results[0].status==='fulfilled'){
  Object.assign(stats,{total:0,normal:0,warning:0,error:0,scrap:0})
  for(const item of results[0].value||[]){const n=Number(item.count)||0;stats.total+=n;const key={1:'normal',2:'warning',3:'error',4:'scrap'}[item.status];if(key)stats[key]+=n}
 }
 const trend=results[1].status==='fulfilled'?results[1].value||[]:[]
 trendPeriod.value=trend.length?String(trend[0].date).slice(0,10)+' 至 '+String(trend[trend.length-1].date).slice(0,10):'近 7 天'
 recentAlerts.value=results[2].status==='fulfilled'?(results[2].value||[]).map(normalize):[]
 pendingOrders.value=results[3].status==='fulfilled'?(results[3].value||[]).map(normalize).filter(x=>x.status>=0&&x.status<4):[]
 await nextTick();renderCharts(trend);loading.value=false
}
onMounted(()=>{loadData();observer=new ResizeObserver(()=>{pieChart?.resize();lineChart?.resize()});observer.observe(pieChartRef.value);observer.observe(lineChartRef.value)})
onBeforeUnmount(()=>{observer?.disconnect();pieChart?.dispose();lineChart?.dispose()})
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4,minmax(0,1fr));
  gap: 15px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 20px;
  min-height: 108px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  background: #ffffffed;
  border: 1px solid #e3eeff;
  border-radius: 14px;
  box-shadow: 0 5px 14px #387cd908;
}

.stat-icon {
  width: 63px;
  height: 63px;
  display: grid;
  place-items: center;
  border: 4px solid #e8f1ff;
  border-radius: 20px;
  box-shadow: 0 4px 10px #3868ed20;
  background: linear-gradient(140deg,#4bb9ff,#5d5bff);
  color: white;
  flex-shrink: 0;
  font-size: 30px;
}

.stat-copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.stat-copy strong {
  font-size: 34px;
  font-weight: 650;
  color: #102752;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.stat-copy span {
  color: #60769b;
  font-size: 14px;
}

.stat-wave {
  position: absolute;
  right: 15px;
  bottom: 12px;
  width: 86px;
  height: 33px;
  color: #438dff;
}

.green .stat-icon {
  background: linear-gradient(135deg,#19c368,#65d6a4);
  border-color: #d9f7e9;
}

.green .stat-wave {
  color: #28c9aa;
}

.amber .stat-icon {
  background: linear-gradient(135deg,#ffa429,#ffca66);
  border-color: #fff3d9;
}

.amber .stat-wave {
  color: #ffac33;
}

.red .stat-icon {
  background: linear-gradient(135deg,#ff5f73,#ff969d);
  border-color: #ffe5e8;
}

.red .stat-wave {
  color: #fc718b;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2,minmax(0,1fr));
  gap: 16px;
}

.chart-card,.records-card {
  padding: 18px;
}

.distribution {
  display: flex;
  align-items: center;
  gap: 20px;
  height: 230px;
}

.donut-chart {
  width: 46%;
  height: 100%;
}

.status-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 23px;
  font-size: 13px;
  padding-right: 16px;
}

.status-legend>div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.legend-label {
  display: flex;
  align-items: center;
  gap: 9px;
}

.legend-label i {
  width: 11px;
  height: 11px;
  border-radius: 50%;
}

.status-legend b {
  margin-left: auto;
  font-weight: 500;
}

.status-legend>div>span:last-child {
  min-width: 40px;
  text-align: right;
}

.trend-chart {
  height: 230px;
}

.small-link {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #657fa8;
  padding: 5px 10px;
  border: 1px solid #dfe9fa;
  border-radius: 6px;
  background: #fafcff;
}

.small-link:hover {
  color: #2777ff;
  background: #eff5ff;
}

.chart-period {
  font-size: 11px;
  color: #6c81a5;
  border: 1px solid #e1eafa;
  border-radius: 6px;
  padding: 6px 8px;
}

.records-card .el-icon {
  color: #4c7cff;
}

.records-card :deep(.el-table th.el-table__cell) {
  height: 39px;
  font-size: 11px;
}

.records-card :deep(.el-table .cell) {
  padding: 0 10px;
}

.records-card :deep(.el-table__empty-block) {
  min-height: 150px;
}

.dashboard-error {
  margin-bottom: 14px;
}

@media (min-width:1600px) {
  .stat-card {
    min-height: 120px;
    padding: 22px;
  }
  .stat-icon {
    width: 74px;
    height: 74px;
    font-size: 34px;
  }
  .stat-copy strong {
    font-size: 39px;
  }
  .stat-copy span {
    font-size: 16px;
  }
  .distribution,.trend-chart {
    height: 250px;
  }
  .chart-card,.records-card {
    padding: 20px;
  }
  .status-legend {
    font-size: 14px;
  }
}

@media (max-width:1150px) {
  .stat-card {
    gap: 12px;
    padding: 15px;
  }
  .stat-icon {
    width: 48px;
    height: 48px;
    font-size: 25px;
    border-radius: 15px;
  }
  .stat-copy strong {
    font-size: 28px;
  }
  .stat-copy span {
    font-size: 12px;
  }
  .distribution {
    gap: 5px;
  }
  .status-legend {
    padding: 0;
    font-size: 11px;
    gap: 22px;
  }
  .status-legend>div {
    gap: 5px;
  }
  .chart-period {
    max-width: 145px;
  }
  .section-heading h2 {
    font-size: 15px;
  }
}

@media (max-width:850px) {
  .stats-grid {
    grid-template-columns: repeat(2,minmax(0,1fr));
  }
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
  .chart-period {
    max-width: none;
  }
  .status-legend {
    font-size: 13px;
  }
  .stat-card {
    min-height: 94px;
  }
}
</style>
