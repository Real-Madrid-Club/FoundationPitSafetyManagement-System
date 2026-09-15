<template>
<div class="workorder">
  <PageHero title="工单调度" description="统一管理设备维修工单，跟踪处理进度，保障设备稳定运行" />
  <section class="surface workorder-panel">
    <div class="filter-bar">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="searchForm.status" placeholder="请选择工单状态" clearable>
            <el-option label="待派单" :value="0" />
            <el-option label="待接单" :value="1" />
            <el-option label="维修中" :value="2" />
            <el-option label="待验收" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="已取消" :value="-1" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">
            <el-icon>
              <Search />
            </el-icon>
            查询
          </el-button>
          <el-button :icon="RefreshRight" @click="handleReset">重置</el-button>
        </el-col>
        <el-col :span="12" class="order-create">
          <el-button v-if="isAdmin || isBuyer" type="primary" :icon="Plus" @click="handleCreateWorkOrder">创建工单</el-button>
        </el-col>
      </el-row>
    </div>
    <div class="table-container">
      <el-table :data="tableData" border stripe style="width: 100%" v-loading="loading">
        <template #empty>
          <EmptyState
            :title="loadError ? '工单加载失败' : '暂无工单数据'"
            :description="loadError ? '请检查连接后重新查询' : (isAdmin || isBuyer) ? '当前暂无匹配的维修工单，点击右上角「创建工单」新建工单' : '当前暂无指派给你的维修工单'"
          />
        </template>
        <el-table-column prop="orderNo" label="工单编号" min-width="165" />
        <el-table-column label="设备编码" width="120">
          <template #default="scope">{{ scope.row.deviceCode || scope.row.deviceId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="faultDesc" label="故障描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="维修人员" width="100">
          <template #default="scope">{{ scope.row.repairerName || scope.row.repairerId || '未分配' }}</template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="报修时间"
          width="165"
          sortable
          :formatter="(_row,_column,value) => value ? String(value).replace('T',' ').slice(0,16) : '—'"
        />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleView(scope.row)">查看</el-button>
            <el-button
              v-if="scope.row.status === 0 && isAdmin"
              type="success"
              size="small"
              @click="handleAssign(scope.row)"
            >
              派单
            </el-button>
            <el-button
              v-if="scope.row.status === 1 && isRepairer"
              type="warning"
              size="small"
              @click="handleStart(scope.row)"
            >
              签到
            </el-button>
            <el-button
              v-if="scope.row.status === 2 && isRepairer"
              type="primary"
              size="small"
              @click="handleComplete(scope.row)"
            >
              完成
            </el-button>
            <el-button
              v-if="scope.row.status === 3 && (isAdmin || isBuyer)"
              type="success"
              size="small"
              @click="handleAccept(scope.row)"
            >
              验收
            </el-button>
            <el-button
              v-if="[0, 1].includes(scope.row.status) && (isAdmin || isBuyer)"
              type="danger"
              size="small"
              @click="handleCancel(scope.row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>
  </section>
  <el-dialog v-model="detailVisible" title="工单详情" width="500px">
    <el-descriptions v-if="currentOrder" :column="2" border>
      <el-descriptions-item label="工单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="设备编码">{{ currentOrder.deviceCode || currentOrder.deviceId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="故障描述">{{ currentOrder.faultDesc }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="getStatusType(currentOrder.status)" size="small">{{ getStatusText(currentOrder.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="维修人员">{{ currentOrder.repairerName || currentOrder.repairerId || '未分配' }}</el-descriptions-item>
      <el-descriptions-item label="报修时间">{{ currentOrder.createTime }}</el-descriptions-item>
      <el-descriptions-item label="维修日志" :span="2">{{ currentOrder.repairLog || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
  <el-dialog v-model="createVisible" title="创建工单" width="500px" :close-on-click-modal="false">
    <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
      <el-form-item label="设备" prop="deviceId">
        <el-select v-model="createForm.deviceId" placeholder="请选择设备" filterable style="width:100%">
          <el-option
            v-for="d in devices"
            :key="d.id"
            :label="d.deviceCode + ' - ' + d.deviceName"
            :value="d.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="故障描述" prop="faultDesc">
        <el-input v-model="createForm.faultDesc" type="textarea" :rows="3" placeholder="请输入故障描述" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" @click="handleCreateSubmit">确定创建</el-button>
    </template>
  </el-dialog>
  <el-dialog v-model="completeVisible" title="填写维修日志" width="500px" :close-on-click-modal="false">
    <el-form :model="completeForm" ref="completeFormRef" label-width="100px">
      <el-form-item label="维修日志" prop="repairLog">
        <el-input v-model="completeForm.repairLog" type="textarea" :rows="4" placeholder="请填写维修日志及耗材" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="completeVisible = false">取消</el-button>
      <el-button type="primary" @click="handleCompleteSubmit">提交</el-button>
    </template>
  </el-dialog>
  <!-- 派单对话框 -->
  <el-dialog v-model="assignVisible" title="派发工单" width="420px" :close-on-click-modal="false">
    <el-form label-width="90px">
      <el-form-item label="工单编号">
        <el-tag>{{ currentOrder?.orderNo }}</el-tag>
      </el-form-item>
      <el-form-item label="设备编码">{{ currentOrder?.deviceCode || currentOrder?.deviceId || '-' }}</el-form-item>
      <el-form-item label="故障描述">{{ currentOrder?.faultDesc }}</el-form-item>
      <el-form-item label="维修工程师">
        <el-select v-model="selectedRepairerId" placeholder="请选择维修工程师" style="width:100%">
          <el-option
            v-for="r in repairers"
            :key="r.id"
            :label="r.realName + ' (' + r.username + ')'"
            :value="r.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="assignVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedRepairerId" @click="handleAssignSubmit">确定派单</el-button>
    </template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'
import {
  getWorkOrderList,
  getWorkOrderDetail,
  createWorkOrder,
  assignWorkOrder,
  startWorkOrder,
  completeWorkOrder,
  acceptWorkOrder,
  cancelWorkOrder
} from '@/api/workorder'
import { getUsers } from '@/api/auth'
import { getDeviceList } from '@/api/device'

const loading = ref(false)
const loadError = ref(false)
const detailVisible = ref(false)
const createVisible = ref(false)
const completeVisible = ref(false)
const assignVisible = ref(false)
const selectedRepairerId = ref(null)
const repairers = ref([])
const devices = ref([])
const currentOrder = ref(null)
const createFormRef = ref()
const completeFormRef = ref()

const searchForm = reactive({ status: null })
const createForm = reactive({ deviceId: null, creatorId: null, faultDesc: '' })
const completeForm = reactive({ repairLog: '' })

const createRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  faultDesc: [{ required: true, message: '请输入故障描述', trigger: 'blur' }]
}

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const getCurrentUserId = () => {
  const userRaw = localStorage.getItem('user')
  if (!userRaw) return null
  try {
    const user = JSON.parse(userRaw)
    return user?.id || user?.userInfo?.id || null
  } catch {
    return null
  }
}

const userRoles = computed(() => {
  const userRaw = localStorage.getItem('user')
  if (!userRaw) return []
  try {
    const user = JSON.parse(userRaw)
    return user?.roles || user?.userInfo?.roles || []
  } catch {
    return []
  }
})
const isAdmin = computed(() => userRoles.value.includes('ROLE_ADMIN'))
const isBuyer = computed(() => userRoles.value.includes('ROLE_BUYER'))
const isRepairer = computed(() => userRoles.value.includes('ROLE_REPAIRER'))

const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'primary', 2: 'warning', 3: 'warning', 4: 'success' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { [-1]: '已取消', 0: '待派单', 1: '待接单', 2: '维修中', 3: '待验收', 4: '已完成' }
  return map[status] || '未知'
}

const loadData = async () => {
  loading.value = true
  loadError.value = false
  try {
    const data = await getWorkOrderList({
      status: searchForm.status != null ? searchForm.status : undefined,
      page: pagination.currentPage,
      pageSize: pagination.pageSize
    })
    if (data && data.list) {
      tableData.value = data.list
      pagination.total = data.total || 0
    } else {
      tableData.value = data || []
      pagination.total = tableData.value.length
    }
  } catch (error) {
    loadError.value = true
    tableData.value = []
    console.error('加载工单列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  searchForm.status = null
  handleSearch()
}

const handleCreateWorkOrder = async () => {
  createForm.deviceId = null
  createForm.creatorId = getCurrentUserId()
  createForm.faultDesc = ''
  await fetchDevices()
  createVisible.value = true
}

const handleCreateSubmit = async () => {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await createWorkOrder({
      deviceId: createForm.deviceId,
      creatorId: getCurrentUserId(),
      faultDesc: createForm.faultDesc
    })
    ElMessage.success('工单创建成功')
    createVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const handleView = async (row) => {
  try {
    currentOrder.value = await getWorkOrderDetail(row.id)
    detailVisible.value = true
  } catch (error) {
    currentOrder.value = row
    detailVisible.value = true
  }
}

const handleAssign = async (row) => {
  currentOrder.value = row
  selectedRepairerId.value = null
  await fetchRepairers()
  assignVisible.value = true
}

const handleAssignSubmit = async () => {
  try {
    await assignWorkOrder(currentOrder.value.id, selectedRepairerId.value)
    ElMessage.success('派单成功')
    assignVisible.value = false
    loadData()
  } catch {
    ElMessage.error('派单失败')
  }
}

const fetchRepairers = async () => {
  try {
    const users = await getUsers()
    repairers.value = users.filter(u => {
      const roles = u.roles || []
      return roles.includes('ROLE_REPAIRER') || roles.includes('现场维修工程师')
    })
  } catch {
    repairers.value = []
  }
}

const fetchDevices = async () => {
  try {
    const data = await getDeviceList({ pageSize: 100 })
    devices.value = data?.list || data || []
  } catch {
    devices.value = []
  }
}

const handleStart = async (row) => {
  try {
    await ElMessageBox.confirm(`确定到场签到并开始维修工单 ${row.orderNo} 吗？`,'签到确认',{
      confirmButtonText: '确定签到', cancelButtonText: '取消', type: 'warning'
    })
    await startWorkOrder(row.id)
    ElMessage.success('已签到，维修中')
    loadData()
  } catch (_) {}
}

const handleComplete = (row) => {
  currentOrder.value = row
  completeForm.repairLog = ''
  completeVisible.value = true
}

const handleCompleteSubmit = async () => {
  try {
    await completeWorkOrder(currentOrder.value.id, completeForm.repairLog)
    ElMessage.success('维修完成，等待验收')
    completeVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const handleAccept = async (row) => {
  try {
    await ElMessageBox.confirm(`确定验收工单 ${row.orderNo} 吗？`,'验收确认',{
      confirmButtonText: '确定验收', cancelButtonText: '取消', type: 'warning'
    })
    await acceptWorkOrder(row.id)
    ElMessage.success('验收完成')
    loadData()
  } catch (_) {}
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(`确定取消工单 ${row.orderNo} 吗？`,'取消确认',{
      confirmButtonText: '确定取消', cancelButtonText: '返回', type: 'warning'
    })
    await cancelWorkOrder(row.id)
    ElMessage.success('工单已取消')
    loadData()
  } catch (_) {}
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.workorder-panel {
  padding: 24px;
}

.workorder .filter-bar {
  padding: 4px 0 22px;
  border: 0;
  box-shadow: none;
  margin: 0;
}

.workorder .table-container {
  padding: 0;
  border: 0;
  box-shadow: none;
}

.order-create {
  display: flex;
  justify-content: flex-end;
}

.workorder :deep(.el-table__empty-block) {
  min-height: clamp(300px,calc(100dvh - 440px),520px);
}

.workorder :deep(.el-table__empty-text) {
  width: 100%;
  line-height: 1.8;
}

.workorder .pagination {
  justify-content: flex-start;
  padding-top: 20px;
}

@media (max-width:1100px) and (min-width:701px) {
  .workorder .filter-bar :deep(.el-col:nth-child(1)) {
    flex: 0 0 32%;
    max-width: 32%;
  }
  .workorder .filter-bar :deep(.el-col:nth-child(2)) {
    flex: 0 0 36%;
    max-width: 36%;
  }
  .workorder .filter-bar :deep(.el-col:nth-child(3)) {
    flex: 0 0 32%;
    max-width: 32%;
  }
}

@media (max-width:700px) {
  .workorder-panel {
    padding: 14px;
  }
  .order-create {
    justify-content: flex-start;
  }
  .workorder :deep(.el-table__empty-block) {
    min-height: 280px;
  }
}
</style>
