<template>
<div class="device-list">
  <PageHero title="设备管理" description="对基坑施工设备进行统一管理，保障设备安全运行" breadcrumb />
  <div class="filter-bar">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-input
          v-model="searchForm.keyword"
          placeholder="设备名称/编码"
          :prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
        />
      </el-col>
      <el-col :span="6">
        <el-select v-model="searchForm.status" placeholder="设备状态" clearable>
          <el-option label="正常" :value="1" />
          <el-option label="异常预警" :value="2" />
          <el-option label="故障待修" :value="3" />
          <el-option label="已报废" :value="4" />
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
      <el-col :span="6" class="device-add">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增设备</el-button>
      </el-col>
    </el-row>
  </div>
  <div class="table-container">
    <el-table :data="tableData" border stripe style="width: 100%" v-loading="loading">
      <template #empty>
        <EmptyState :title="loadError ? '设备加载失败' : '暂无设备数据'" description="请调整筛选条件，或新增设备" compact />
      </template>
      <el-table-column prop="deviceCode" label="设备编码" width="120" />
      <el-table-column prop="deviceName" label="设备名称" min-width="150" />
      <el-table-column prop="deviceType" label="设备类型" width="140" />
      <el-table-column
        prop="ipGrade"
        label="防护等级"
        width="100"
        :formatter="(_row,_column,value) => value || '—'"
      />
      <el-table-column prop="maintenanceCycle" label="维保周期(天)" width="120" />
      <el-table-column prop="status" label="设备状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)" size="small">
            <i class="status-dot"></i>
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="入库时间" width="135" :formatter="formatDate" sortable />
      <el-table-column label="操作" width="270" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="warning" size="small" :icon="Tools" @click="handleStatusChange(scope.row)">
            {{ scope.row.status === 1 ? '设为故障' : '设为正常' }}
          </el-button>
          <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <span class="record-count">共 {{ pagination.total }} 条记录</span>
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10,20,50]"
        layout="prev, pager, next, sizes"
        @current-change="loadData"
        @size-change="handleSearch"
      />
    </div>
  </div>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
    <el-form :model="formData" :rules="formRules" ref="formRef" label-width="110px">
      <el-form-item label="设备编码" prop="deviceCode">
        <el-input v-model="formData.deviceCode" placeholder="请输入设备编码" />
      </el-form-item>
      <el-form-item label="设备名称" prop="deviceName">
        <el-input v-model="formData.deviceName" placeholder="请输入设备名称" />
      </el-form-item>
      <el-form-item label="设备类型" prop="deviceType">
        <el-select v-model="formData.deviceType" placeholder="请选择设备类型" style="width: 100%">
          <el-option label="全站仪" value="全站仪" />
          <el-option label="伺服轴力计" value="伺服轴力计" />
          <el-option label="温度传感器" value="温度传感器" />
          <el-option label="测斜仪" value="测斜仪" />
          <el-option label="水位计" value="水位计" />
          <el-option label="土压力计" value="土压力计" />
          <el-option label="传感器" value="传感器" />
        </el-select>
      </el-form-item>
      <el-form-item label="防护等级" prop="ipGrade">
        <el-input v-model="formData.ipGrade" placeholder="如 IP68" />
      </el-form-item>
      <el-form-item label="维保周期(天)" prop="maintenanceCycle">
        <el-input-number v-model="formData.maintenanceCycle" :min="1" :max="365" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight, Edit, Tools, Delete } from '@element-plus/icons-vue'
import { getDeviceList, addDevice, updateDevice, deleteDevice, updateDeviceStatus } from '@/api/device'

const loading = ref(false)
const loadError = ref(false)
const formatDate = (_row,_column,value) => value ? String(value).slice(0,10) : '—'
const dialogVisible = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingRow = ref(null)

const formData = reactive({
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  ipGrade: '',
  maintenanceCycle: 30
})

const formRules = {
  deviceCode: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }]
}

const searchForm = reactive({
  keyword: '',
  status: null
})

const tableData = ref([])
const pagination = reactive({
  currentPage: 1, pageSize: 10, total: 0
})

const getStatusType = (status) => {
  const map = { 1: 'success', 2: 'warning', 3: 'danger', 4: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 1: '正常', 2: '异常预警', 3: '故障待修', 4: '已报废' }
  return map[status] || '未知'
}

const loadData = async () => {
  loading.value = true
  loadError.value = false
  try {
    const data = await getDeviceList({
      page: pagination.currentPage, pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status || undefined
    })
    if (data && Array.isArray(data.list)) {
      tableData.value = data.list
      pagination.total = data.total || 0
    } else {
      tableData.value = Array.isArray(data) ? data : []
      pagination.total = tableData.value.length
    }
  } catch (error) {
    loadError.value = true
    tableData.value = []
    console.error('加载设备列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = null
  handleSearch()
}

const dialogTitle = computed(() => isEdit.value ? '编辑设备' : '新增设备')

const handleAdd = () => {
  isEdit.value = false
  editingRow.value = null
  formData.deviceCode = ''
  formData.deviceName = ''
  formData.deviceType = ''
  formData.ipGrade = ''
  formData.maintenanceCycle = 30
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editingRow.value = row
  formData.deviceCode = row.deviceCode
  formData.deviceName = row.deviceName
  formData.deviceType = row.deviceType
  formData.ipGrade = row.ipGrade || ''
  formData.maintenanceCycle = row.maintenanceCycle || 30
  dialogVisible.value = true
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value && editingRow.value) {
        await updateDevice({ id: editingRow.value.id, ...formData })
        ElMessage.success('设备信息更新成功')
      } else {
        await addDevice({ ...formData })
        ElMessage.success('新设备添加成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (error) {
      ElMessage.error('操作失败')
    }
  })
}

const handleStatusChange = async (row) => {
  const newStatus = row.status === 1 ? 3 : 1
  const statusText = row.status === 1 ? '故障待修' : '正常'
  try {
    await ElMessageBox.confirm(
      `确定将设备 ${row.deviceName} 状态设为 ${statusText} 吗？`,
      '状态变更确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await updateDeviceStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success('状态修改成功')
  } catch (_) {
    if (_.toString() !== 'cancel') {
      ElMessage.error('状态修改失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除设备 ${row.deviceName} 吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteDevice(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (_) {
    if (_.toString() !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.device-add {
  display: flex;
  justify-content: flex-end;
}

.record-count {
  margin-right: auto;
  color: #6f85ac;
  font-size: 14px;
  white-space: nowrap;
}

.device-list :deep(.el-table__empty-block) {
  min-height: 240px;
}

@media (max-width:1100px) and (min-width:701px) {
  .filter-bar :deep(.el-col) {
    flex: 0 0 50%;
    max-width: 50%;
  }
  .filter-bar :deep(.el-row) {
    row-gap: 14px;
  }
}

@media (max-width:700px) {
  .device-add {
    justify-content: flex-start;
  }
  .record-count {
    display: none;
  }
}
</style>
