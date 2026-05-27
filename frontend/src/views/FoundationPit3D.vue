<template>
  <div class="pit-3d-page">
    <div class="canvas-wrap" ref="canvasWrap">
      <!-- 加载进度遮罩 -->
      <div v-if="loadingPercent < 100" class="loading-overlay">
        <div class="loading-box">
          <div class="loader-ring"></div>
          <h3>基坑模型加载中</h3>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: loadingPercent + '%' }"></div>
          </div>
          <p>{{ loadingPercent }}%</p>
        </div>
      </div>
      <div class="legend-bar">
        <div class="legend-group">
          <span class="leg"><b>◆</b> 轴力计(圆柱)</span>
          <span class="leg"><b>▲</b> 全站仪(锥体)</span>
          <span class="leg"><b>■</b> 温度传感器(方块)</span>
        </div>
        <div class="legend-group">
          <span class="leg"><i style="background:#44bb44"></i> 正常</span>
          <span class="leg"><i style="background:#ff9900"></i> 预警</span>
          <span class="leg"><i style="background:#ff4444"></i> 故障</span>
          <span class="leg"><i style="background:#999999"></i> 报废</span>
        </div>
        <div class="slider-group">
          <span class="leg">整体高度:</span>
          <input type="range" min="-20" max="20" step="0.5" :value="yOffset" @input="onYOffsetChange"
            class="y-slider" title="拖拽调整所有传感器高度" />
          <span class="leg" style="min-width:40px">{{ yOffset }}</span>
        </div>
      </div>

      <!-- 设备侧边栏 -->
      <div class="sensor-panel">
        <input v-model="searchText" placeholder="🔍 搜索设备..." class="panel-search" />
        <div class="panel-list">
          <template v-for="group in groupedSensors" :key="group.label">
            <div class="panel-group-title" :style="{ color: group.color }" @click="toggleGroup(group)">
              {{ group.icon }} {{ group.label }}
              <span class="group-toggle">{{ groupAllVisible(group) ? '☑' : '☐' }}</span>
            </div>
            <div v-for="s in group.items" :key="s.code" class="panel-item"
              :class="{ selected: selectedCode === s.code, hidden: !visibleMap[s.code] }"
              @click="onPanelClick(s)">
              <input type="checkbox" :checked="visibleMap[s.code]" @click.stop @change="toggleSensor(s.code)" />
              <span class="item-dot" :style="{ background: getSensorColor(s.code) }"></span>
              <span class="item-label">{{ s.label }}</span>
              <span class="item-hp" :style="{ color: getHealthColor(s.code) }">{{ getHealthPct(s.code) }}%</span>
            </div>
          </template>
          <div v-if="groupedSensors.length === 0" class="panel-empty">无匹配设备</div>
        </div>
      </div>
    </div>

    <!-- 传感器数据图表弹窗 -->
    <el-dialog v-model="showChart" :title="selectedSensor" width="750px" destroy-on-close>
      <div ref="chartRef" style="width:100%;height:350px"></div>
      <el-pagination
        v-if="chartTotal > chartPageSize"
        small
        layout="prev, pager, next"
        :total="chartTotal"
        :page-size="chartPageSize"
        @current-change="loadChartData"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch, reactive, computed } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js'
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js'
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'
import * as echarts from 'echarts'
import { getTotalStation, getAxialForce, getSteelTemperature } from '@/api/monitor'
import { getDeviceList } from '@/api/device'
import request from '@/api/request'

// ---- 健康状态颜色 ----
const HEALTH_COLORS = { 1: 0x44bb44, 2: 0xff9900, 3: 0xff4444, 4: 0x999999 }

// ---- 传感器定义 ----
const SENSOR_DEFS = [
  // 伺服轴力计 (圆柱)
  { code: 'SP1',  type: 'axialForce',     label: 'SP1 轴力计' },
  { code: '4P1',  type: 'axialForce',     label: '4P1 轴力计' },
  { code: '4P2',  type: 'axialForce',     label: '4P2 轴力计' },
  // 全站仪 (锥体) — 8台
  { code: 'FRHY-01', type: 'totalStation', label: 'FRHY-01 全站仪' },
  { code: 'FRHY-02', type: 'totalStation', label: 'FRHY-02 全站仪' },
  { code: 'FRHY-03', type: 'totalStation', label: 'FRHY-03 全站仪' },
  { code: 'FRHY-04', type: 'totalStation', label: 'FRHY-04 全站仪' },
  { code: 'FRHY-05', type: 'totalStation', label: 'FRHY-05 全站仪' },
  { code: 'FRHY-06', type: 'totalStation', label: 'FRHY-06 全站仪' },
  { code: 'FRHY-07', type: 'totalStation', label: 'FRHY-07 全站仪' },
  { code: 'HSD-01',  type: 'totalStation', label: 'HSD-01 全站仪' },
  { code: 'HSD-02',  type: 'totalStation', label: 'HSD-02 全站仪' },
  { code: 'HSD-03',  type: 'totalStation', label: 'HSD-03 全站仪' },
  // 温度传感器 (方块) — 8台
  { code: '6501945', type: 'steelTemp',    label: '6501945 温度' },
  { code: '6501947', type: 'steelTemp',    label: '6501947 温度' },
  { code: '6501952', type: 'steelTemp',    label: '6501952 温度' },
  { code: '6501957', type: 'steelTemp',    label: '6501957 温度' },
  { code: '6501959', type: 'steelTemp',    label: '6501959 温度' },
  { code: '6501962', type: 'steelTemp',    label: '6501962 温度' },
  { code: '6501965', type: 'steelTemp',    label: '6501965 温度' },
  { code: '6501968', type: 'steelTemp',    label: '6501968 温度' },
  // 其他传感器 (方块)
  { code: '6501955', type: 'steelTemp',    label: '6501955 平台测点5' },
  { code: '6501961', type: 'steelTemp',    label: '6501961 平台测点4' },
]

let deviceStatusMap = {} // { 'SP1': 1, 'FRHY-01': 2, ... }

// ---- refs ----
const canvasWrap = ref(null)
const showChart = ref(false)
const selectedSensor = ref('')
const chartRef = ref(null)
const chartTotal = ref(0)
const chartPageSize = ref(50)
const loadingPercent = ref(0)
const yOffset = ref(0)
const searchText = ref('')
const selectedCode = ref('')
let chartInstance = null
let currentSensorCode = ''

const visibleMap = reactive({})
const markerMap = {}

// ---- Three.js 对象 ----
let scene, camera, renderer, labelRenderer, controls, raycaster, mouse
let modelGroup, sensorGroup
let animationId

const sensorMarkers = [] // { code, mesh, ring, type }

// ---- 加载保存的传感器位置 ----
function loadPositions() {
  try {
    const raw = localStorage.getItem('pit_sensor_positions')
    return raw ? JSON.parse(raw) : {}
  } catch { return {} }
}
function savePosition(code, vec) {
  const pos = loadPositions()
  pos[code] = { x: vec.x, y: vec.y, z: vec.z }
  localStorage.setItem('pit_sensor_positions', JSON.stringify(pos))
}

// ---- 计算默认传感器坐标 ----
function computeDefaultPositions(box) {
  const w = box.max.x - box.min.x
  const d = box.max.z - box.min.z
  const h = box.max.y - box.min.y
  const mx = box.min.x, mz = box.min.z
  const bottomY = box.min.y + h * 0.1   // 坑底附近
  const lowY = box.min.y + h * 0.3      // 下部
  const midY = box.min.y + h * 0.5      // 中部
  const highY = box.min.y + h * 0.65    // 上部支撑梁

  const defaults = {}

  // 轴力计(3台) – 上部支撑梁，x方向排开
  const axialCodes = ['SP1', '4P1', '4P2']
  axialCodes.forEach((code, i) => {
    defaults[code] = { x: mx + w * (0.25 + i * 0.25), y: highY, z: mz + d * 0.15 }
  })

  // 全站仪(11台) – 沿坑壁四周，高度递减
  const stationCodes = SENSOR_DEFS.filter(s => s.type === 'totalStation').map(s => s.code)
  stationCodes.forEach((code, i) => {
    const side = i % 4
    const along = Math.floor(i / 4) / 3
    const depth = i < 4 ? highY : i < 8 ? midY : lowY  // 前4个在上部，中4个在中间，最后在底部
    let x, z
    if (side === 0) { x = mx + w * (0.1 + along * 0.8); z = mz }
    else if (side === 1) { x = mx + w; z = mz + d * (0.1 + along * 0.8) }
    else if (side === 2) { x = mx + w * (0.9 - along * 0.8); z = mz + d }
    else { x = mx; z = mz + d * (0.9 - along * 0.8) }
    defaults[code] = { x, y: depth, z }
  })

  // 温度/其他传感器(10台) – 网格分布，中下部
  const tempCodes = SENSOR_DEFS.filter(s => s.type === 'steelTemp').map(s => s.code)
  const cols = 4
  tempCodes.forEach((code, i) => {
    const col = i % cols, row = Math.floor(i / cols)
    defaults[code] = {
      x: mx + w * (0.15 + col * 0.23),
      y: box.min.y + h * (0.2 + row * 0.25),
      z: mz + d * (0.2 + Math.floor(row / 2) * 0.3),
    }
  })

  // 已保存坐标覆盖
  const saved = loadPositions()
  SENSOR_DEFS.forEach(s => {
    if (saved[s.code]) defaults[s.code] = saved[s.code]
  })
  return defaults
}

// ---- 创建传感器标记 ----
function createMarkers(modelGroup, box) {
  const positions = computeDefaultPositions(box)
  SENSOR_DEFS.forEach(def => {
    const pos = positions[def.code] || { x: 0, y: box.max.y, z: 0 }
    const status = deviceStatusMap[def.code] || 1
    const color = HEALTH_COLORS[status] || 0x888888

    let geo
    if (def.type === 'axialForce') {
      geo = new THREE.CylinderGeometry(0.2, 0.2, 0.7, 12)
    } else if (def.type === 'totalStation') {
      geo = new THREE.ConeGeometry(0.25, 0.6, 8)
    } else {
      geo = new THREE.BoxGeometry(0.4, 0.4, 0.4)
    }

    const mat = new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.5 })
    const mesh = new THREE.Mesh(geo, mat)
    mesh.position.set(pos.x, pos.y, pos.z)
    mesh.userData = { sensorCode: def.code, sensorType: def.type, isSensor: true, baseY: pos.y }
    modelGroup.add(mesh)

    const ringGeo = new THREE.TorusGeometry(0.35, 0.04, 8, 16)
    const ringMat = new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.4, transparent: true, opacity: 0.6 })
    const ring = new THREE.Mesh(ringGeo, ringMat)
    ring.position.set(pos.x, pos.y, pos.z)
    ring.userData = { baseY: pos.y }
    modelGroup.add(ring)

    sensorMarkers.push({ code: def.code, mesh, ring, type: def.type })
    markerMap[def.code] = { mesh, ring }
    if (!(def.code in visibleMap)) visibleMap[def.code] = true
    createHealthBar(def.code, '#' + color.toString(16).padStart(6, '0'))
  })
}

// ---- Y 轴整体偏移 ----
function onYOffsetChange(e) {
  const val = parseFloat(e.target.value)
  yOffset.value = val
  sensorMarkers.forEach(s => {
    const by = s.mesh.userData?.baseY ?? s.mesh.position.y
    s.mesh.position.y = by + val
    if (s.ring) s.ring.position.y = by + val
  })
  SENSOR_DEFS.forEach(s => positionHealthBar(s.code, val))
  localStorage.setItem('pit_y_offset', val)
}

// ---- 侧边栏面板逻辑 ----
const TYPE_CONFIG = {
  axialForce: { label: '伺服轴力计', icon: '◆', color: '#ff6666' },
  totalStation: { label: '全站仪', icon: '▲', color: '#6699ff' },
  steelTemp: { label: '温度传感器', icon: '■', color: '#66cc66' },
}

const groupedSensors = computed(() => {
  const groups = {}
  SENSOR_DEFS.forEach(s => {
    if (searchText.value && !s.label.includes(searchText.value) && !s.code.includes(searchText.value)) return
    const t = TYPE_CONFIG[s.type]
    if (!groups[s.type]) groups[s.type] = { ...t, items: [] }
    groups[s.type].items.push(s)
  })
  return Object.values(groups)
})

const groupAllVisible = (group) => group.items.every(s => visibleMap[s.code])

function toggleSensor(code) {
  visibleMap[code] = !visibleMap[code]
  const m = markerMap[code]
  if (m) {
    m.mesh.visible = visibleMap[code]
    if (m.ring) m.ring.visible = visibleMap[code]
  }
}

function toggleGroup(group) {
  const allOn = groupAllVisible(group)
  group.items.forEach(s => {
    visibleMap[s.code] = !allOn
    const m = markerMap[s.code]
    if (m) {
      m.mesh.visible = !allOn
      if (m.ring) m.ring.visible = !allOn
    }
  })
}

function getSensorColor(code) {
  const status = deviceStatusMap[code] || 1
  const hex = HEALTH_COLORS[status] ? '#' + HEALTH_COLORS[status].toString(16).padStart(6, '0') : '#888'
  return hex
}

function getHealthPct(code) {
  return healthDataMap[code]?.healthPercent ?? '--'
}

function getHealthColor(code) {
  const s = healthDataMap[code]?.status
  return s === 'danger' ? '#ff4444' : s === 'warning' ? '#ff9900' : '#44bb44'
}

function onPanelClick(s) {
  selectedCode.value = s.code
  const m = markerMap[s.code]
  if (!m) return
  // 相机飞向传感器
  const target = m.mesh.position.clone()
  controls.target.lerp(target, 0.5)
  camera.position.lerp(new THREE.Vector3(target.x + 8, target.y + 5, target.z + 8), 0.5)
  // 高亮闪烁
  const origIntensity = m.mesh.material.emissiveIntensity || 0.5
  m.mesh.material.emissiveIntensity = 1.5
  setTimeout(() => { if (m.mesh.material) m.mesh.material.emissiveIntensity = origIntensity }, 800)
}

// ---- 健康度血条 ----
const healthDataMap = reactive({}) // { code: { healthPercent, status, ... } }
const healthLabels = {} // code → CSS2DObject

function createHealthBar(code, color) {
  const div = document.createElement('div')
  div.className = 'health-bar-3d'
  div.innerHTML = `
    <div class="hb-track"><div class="hb-fill" style="width:100%;background:${color}"></div></div>
    <div class="hb-text">100%</div>
  `
  const label = new CSS2DObject(div)
  label.position.set(0, 0, 0)
  label.visible = true
  modelGroup.add(label)
  healthLabels[code] = label
}

function updateHealthBar(code, pct, status) {
  const label = healthLabels[code]
  if (!label) return
  const div = label.element
  const colors = { danger: '#ff4444', warning: '#ff9900', normal: '#44bb44' }
  const c = colors[status] || '#44bb44'
  div.querySelector('.hb-fill').style.width = pct + '%'
  div.querySelector('.hb-fill').style.background = c
  div.querySelector('.hb-text').textContent = pct + '%'
  div.querySelector('.hb-text').style.color = c
}

function positionHealthBar(code, yOffsetVal) {
  const m = markerMap[code]
  const label = healthLabels[code]
  if (!m || !label) return
  const pos = m.mesh.position
  label.position.set(pos.x, pos.y + 1.2 + yOffsetVal * 0.05, pos.z)
}

async function fetchAllHealth() {
  try {
    const list = await request.get('/health/all')
    if (Array.isArray(list)) {
      list.forEach(d => {
        healthDataMap[d.sensorCode] = d
        updateHealthBar(d.sensorCode, d.healthPercent, d.status)
      })
    }
  } catch { /* ignore */ }
}

// ---- 射线检测 ----
function onCanvasClick(event) {
  if (!raycaster || !sensorMarkers.length) return

  const rect = renderer.domElement.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(mouse, camera)
  const targets = sensorMarkers.map(s => s.mesh)
  const intersects = raycaster.intersectObjects(targets)
  if (intersects.length > 0) {
    const obj = intersects[0].object
    if (obj.userData?.isSensor) {
      currentSensorCode = obj.userData.sensorCode
      selectedSensor.value = SENSOR_DEFS.find(s => s.code === currentSensorCode)?.label || currentSensorCode
      showChart.value = true
      nextTick(() => { chartTotal.value = 0; loadChartData(1) })
    }
  }
}

// 弹窗关闭时释放图表实例
watch(showChart, (val) => {
  if (!val) {
    if (chartInstance) { chartInstance.dispose(); chartInstance = null }
  }
})

// ---- 拖拽传感器 ----
let dragTarget = null
let dragPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0)

function onMouseDown(event) {
  if (!event.shiftKey || !raycaster) return
  const rect = renderer.domElement.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(mouse, camera)
  const targets = sensorMarkers.map(s => s.mesh)
  const hits = raycaster.intersectObjects(targets)
  if (hits.length > 0) {
    controls.enabled = false
    dragTarget = hits[0].object
    const p = dragTarget.position
    dragPlane.constant = -p.y
  }
}
function onMouseMove(event) {
  if (!dragTarget) return
  const rect = renderer.domElement.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(mouse, camera)
  const pt = new THREE.Vector3()
  raycaster.ray.intersectPlane(dragPlane, pt)
  if (pt) {
    dragTarget.position.copy(pt)
    if (dragTarget.userData?.ring) {
      dragTarget.userData.ring.position.copy(pt)
    }
  }
}
function onMouseUp() {
  if (dragTarget) {
    const code = dragTarget.userData?.sensorCode
    if (code) {
      // 保存时去掉Y偏移，存原始baseY
      const pos = dragTarget.position.clone()
      pos.y -= yOffset.value
      savePosition(code, pos)
      dragTarget.userData.baseY = pos.y
    }
    dragTarget = null
    controls.enabled = true
  }
}

// ---- 图表数据加载 ----
async function loadChartData(page = 1) {
  if (!currentSensorCode) return
  const code = currentSensorCode
  const start = '2025-01-01 00:00:00'
  const end = '2026-12-31 23:59:59'

  let data
  try {
    if (code.startsWith('FRHY') || code.startsWith('HSD')) {
      data = await getTotalStation({ sensorCode: code, startTime: start, endTime: end, page, pageSize: chartPageSize.value })
    } else if (code.match(/^(SP|4P)/)) {
      data = await getAxialForce({ sensorCode: code, startTime: start, endTime: end, page, pageSize: chartPageSize.value })
    } else {
      data = await getSteelTemperature({ sensorCode: code, startTime: start, endTime: end, page, pageSize: chartPageSize.value })
    }
  } catch (e) {
    console.error('加载传感器数据失败:', e)
    return
  }

  const list = data?.list || (Array.isArray(data) ? data : [])
  if (!list.length) return
  chartTotal.value = data?.total || list.length

  const yField = (code.startsWith('FRHY') || code.startsWith('HSD')) ? 'totalX' : code.match(/^(SP|4P)/) ? 'wForce' : 'temperature'
  const yLabel = (code.startsWith('FRHY') || code.startsWith('HSD')) ? '累计X位移(mm)' : code.match(/^(SP|4P)/) ? '轴力(kN)' : '温度(℃)'

  const getVal = (item, field) => {
    if (item[field] !== undefined) return item[field]
    const low = field.toLowerCase()
    if (item[low] !== undefined) return item[low]
    if (item['wForce'] !== undefined && low === 'wforce') return item['wForce']
    if (item['wforce'] !== undefined && low === 'wforce') return item['wforce']
    return 0
  }

  const times = list.map(item => item.collectTime || item.collect_time || '').reverse()
  const values = list.map(item => getVal(item, yField)).reverse()

  await nextTick()
  if (!chartRef.value) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: times, boundaryGap: false, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: yLabel },
    dataZoom: [{ type: 'inside' }, { type: 'slider', height: 20, bottom: 0 }],
    series: [{ data: values, type: 'line', smooth: true, showSymbol: false, color: '#409EFF', areaStyle: { opacity: 0.1 } }],
  })
}

// ---- 拉取设备健康状态 ----
async function fetchDeviceStatus() {
  try {
    const data = await getDeviceList({ pageSize: 100 })
    const list = data?.list || (Array.isArray(data) ? data : [])
    list.forEach(d => {
      if (d.deviceCode) deviceStatusMap[d.deviceCode] = d.status || 1
    })
  } catch { /* 默认全正常 */ }
}

// ---- 初始化 Three.js ----
onMounted(async () => {
  // 恢复保存的 Y 偏移
  const savedY = localStorage.getItem('pit_y_offset')
  if (savedY !== null) yOffset.value = parseFloat(savedY)

  // 先拉取设备状态
  await fetchDeviceStatus()

  const wrap = canvasWrap.value
  if (!wrap) return

  // 场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x2a2a3e)

  // 相机
  camera = new THREE.PerspectiveCamera(60, wrap.clientWidth / wrap.clientHeight, 0.1, 500)
  camera.position.set(20, 15, 25)
  camera.lookAt(0, 0, 0)

  // 渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(wrap.clientWidth, wrap.clientHeight)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  wrap.appendChild(renderer.domElement)

  // CSS2D 渲染器（血条标签）
  labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(wrap.clientWidth, wrap.clientHeight)
  labelRenderer.domElement.style.position = 'absolute'
  labelRenderer.domElement.style.top = '0'
  labelRenderer.domElement.style.pointerEvents = 'none'
  wrap.appendChild(labelRenderer.domElement)

  // 光照
  scene.add(new THREE.AmbientLight(0xffffff, 1.2))
  scene.add(new THREE.HemisphereLight(0xffffff, 0x8899cc, 0.7))
  const dir = new THREE.DirectionalLight(0xffffff, 1.8)
  dir.position.set(10, 20, 10)
  dir.castShadow = true
  scene.add(dir)
  const dir2 = new THREE.DirectionalLight(0x8888ff, 0.4)
  dir2.position.set(-5, 5, -5)
  scene.add(dir2)

  // 控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.target.set(0, 0, 0)

  // 射线
  raycaster = new THREE.Raycaster()
  raycaster.params.Points.threshold = 0.5
  raycaster.params.Line = { threshold: 0.5 }
  mouse = new THREE.Vector2()

  // 传感器组
  sensorGroup = new THREE.Group()
  modelGroup = new THREE.Group()
  scene.add(modelGroup)

  // 网格辅助
  const grid = new THREE.GridHelper(30, 20, 0x555577, 0x333355)
  scene.add(grid)

  // 坐标轴（红X 绿Y 蓝Z）
  scene.add(new THREE.AxesHelper(5))

  // 提前启动渲染循环（模型加载前也能看到网格）
  requestAnimationFrame(animate)

  // 加载模型
  const loader = new GLTFLoader()
  const dracoLoader = new DRACOLoader()
  dracoLoader.setDecoderPath('/draco/')
  loader.setDRACOLoader(dracoLoader)
  loader.load('/Foundation-pit.glb', (gltf) => {
    loadingPercent.value = 100
    const model = gltf.scene
    let meshCount = 0
    model.traverse(child => {
      if (child.isMesh) {
        meshCount++
        child.castShadow = true
        child.receiveShadow = true
        // 修复过暗材质
        if (child.material && child.material.color) {
          child.material.needsUpdate = true
        }
      }
    })
    console.log('模型加载完成, mesh数:', meshCount)

    // 适配画布尺寸
    const box = new THREE.Box3().setFromObject(model)
    console.log('原始BBox min:', box.min, 'max:', box.max)
    const size = Math.max(box.max.x - box.min.x, box.max.y - box.min.y, box.max.z - box.min.z)
    const scale = size > 0 && isFinite(size) ? 15 / size : 1
    console.log('BBox size:', size, 'scale:', scale)
    model.scale.setScalar(scale)

    // 更新BBox并居中
    const box2 = new THREE.Box3().setFromObject(model)
    const cx = (box2.min.x + box2.max.x) / 2
    const cy = (box2.min.y + box2.max.y) / 2
    const cz = (box2.min.z + box2.max.z) / 2
    model.position.set(-cx, -cy, -cz)
    console.log('模型居中偏移:', -cx, -cy, -cz)

    // 调试：BoundingBox 线框
    const boxGeo = new THREE.BoxGeometry(box2.max.x - box2.min.x, box2.max.y - box2.min.y, box2.max.z - box2.min.z)
    const boxWire = new THREE.LineSegments(
      new THREE.EdgesGeometry(boxGeo),
      new THREE.LineBasicMaterial({ color: 0x00ff00 })
    )
    boxWire.position.set(0, 0, 0)
    modelGroup.add(boxWire)
    console.log('线框已添加，尺寸:', box2.max.x - box2.min.x, box2.max.y - box2.min.y, box2.max.z - box2.min.z)

    modelGroup.add(model)

    // 相机看向模型中心
    const center = new THREE.Vector3(0, 0, 0)
    controls.target.copy(center)
    camera.position.set(12, 8, 18)
    camera.lookAt(center)
    controls.update()

    // 创建传感器
    createMarkers(modelGroup, box2)

    // 拉取健康预测数据
    fetchAllHealth().then(() => {
      SENSOR_DEFS.forEach(s => positionHealthBar(s.code, yOffset.value))
    })

    // 首次加载后点击一次空白处触发render
    requestAnimationFrame(animate)
  }, (xhr) => {
    if (xhr.total) {
      loadingPercent.value = Math.round((xhr.loaded / xhr.total) * 100)
    } else if (xhr.loaded) {
      loadingPercent.value = Math.min(99, Math.round(xhr.loaded / 1024 / 1024 / 130 * 100))
    }
  }, () => {
    loadingPercent.value = 100
    // 模型加载失败也启动渲染循环
    requestAnimationFrame(animate)
  })

  // 事件
  renderer.domElement.addEventListener('click', onCanvasClick)
  renderer.domElement.addEventListener('mousedown', onMouseDown)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
  window.addEventListener('resize', () => {
    if (!wrap) return
    camera.aspect = wrap.clientWidth / wrap.clientHeight
    camera.updateProjectionMatrix()
    renderer.setSize(wrap.clientWidth, wrap.clientHeight)
    if (labelRenderer) labelRenderer.setSize(wrap.clientWidth, wrap.clientHeight)
  })
})

function animate() {
  animationId = requestAnimationFrame(animate)
  controls.update()
  // 传感器光环旋转动画
  const t = performance.now() * 0.001
  sensorMarkers.forEach(s => {
    if (s.ring) {
      s.ring.rotation.x = Math.sin(t * 1.5) * 0.3
      s.ring.rotation.y += 0.02
    }
  })
  renderer.render(scene, camera)
  if (labelRenderer) labelRenderer.render(scene, camera)
}

onBeforeUnmount(() => {
  cancelAnimationFrame(animationId)
  if (renderer) renderer.dispose()
  if (chartInstance) chartInstance.dispose()
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', onMouseUp)
})
</script>

<style scoped>
.pit-3d-page {
  position: relative;
  height: calc(100vh - 100px);
  min-height: 500px;
  background: #0f0f1a;
  border-radius: 8px;
  overflow: hidden;
}
.canvas-wrap {
  width: 100%;
  height: 100%;
  position: relative;
}
.canvas-wrap :deep(canvas) {
  display: block;
}
.legend-bar {
  position: absolute;
  top: 12px;
  left: 16px;
  z-index: 10;
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
.legend-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding-right: 16px;
  border-right: 1px solid #333;
}
.legend-group:last-of-type {
  border-right: none;
}
.leg {
  color: #ccc;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.leg i {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
}
.leg b {
  font-size: 16px;
  color: #aaa;
}
.leg.hint {
  color: #888;
}

.slider-group {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  background: rgba(255,255,255,0.05);
  border-radius: 6px;
}
.y-slider {
  width: 100px;
  accent-color: #409EFF;
}

/* 设备侧边栏 */
.sensor-panel {
  position: absolute;
  left: 10px;
  top: 90px;
  bottom: 10px;
  width: 200px;
  background: rgba(20, 20, 40, 0.88);
  border-radius: 8px;
  border: 1px solid #333;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 10;
}
.panel-search {
  margin: 8px;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #444;
  background: rgba(255,255,255,0.06);
  color: #ccc;
  font-size: 13px;
  outline: none;
}
.panel-search::placeholder { color: #666; }
.panel-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 6px 8px;
}
.panel-list::-webkit-scrollbar { width: 4px; }
.panel-list::-webkit-scrollbar-thumb { background: #444; border-radius: 2px; }
.panel-group-title {
  padding: 6px 4px 3px;
  font-size: 12px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  user-select: none;
}
.panel-group-title:hover { opacity: 0.8; }
.group-toggle { font-size: 14px; }
.panel-item {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 4px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  color: #bbb;
  transition: background 0.15s;
}
.panel-item:hover { background: rgba(255,255,255,0.06); }
.panel-item.selected { background: rgba(64,158,255,0.15); }
.panel-item.hidden { opacity: 0.35; }
.panel-item input[type="checkbox"] { accent-color: #409EFF; transform: scale(0.9); }
.item-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.item-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-status {
  font-size: 8px;
}
.item-hp {
  font-size: 11px;
  font-weight: bold;
  min-width: 32px;
  text-align: right;
}
.panel-empty {
  text-align: center;
  padding: 20px;
  color: #666;
  font-size: 13px;
}
</style>

<!-- 3D血条全局样式（CSS2DRenderer 渲染，不能用 scoped） -->
<style>
.health-bar-3d {
  background: rgba(0,0,0,0.7);
  border-radius: 6px;
  padding: 3px 8px;
  font-size: 11px;
  color: #fff;
  text-align: center;
  transform: translate(-50%, -50%);
  white-space: nowrap;
  pointer-events: none;
}
.hb-track {
  width: 50px;
  height: 5px;
  background: #333;
  border-radius: 3px;
  margin: 2px auto;
  overflow: hidden;
}
.hb-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s, background 0.5s;
}
.hb-text {
  font-size: 10px;
  font-weight: bold;
  line-height: 1;
}

/* 加载进度遮罩 */
.loading-overlay {
  position: absolute;
  inset: 0;
  z-index: 20;
  background: rgba(10, 10, 26, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
}
.loading-box {
  text-align: center;
  color: #ccc;
}
.loading-box h3 {
  color: #fff;
  margin: 12px 0 8px;
  font-weight: 400;
}
.loader-ring {
  width: 48px;
  height: 48px;
  border: 4px solid #333;
  border-top-color: #409EFF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto;
}
@keyframes spin { to { transform: rotate(360deg); } }
.progress-bar {
  width: 260px;
  height: 8px;
  background: #333;
  border-radius: 4px;
  margin: 16px auto 8px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #409EFF, #67C23A);
  border-radius: 4px;
  transition: width 0.3s;
}
.loading-box p {
  font-size: 14px;
  color: #888;
}
</style>
