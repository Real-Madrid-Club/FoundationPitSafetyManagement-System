<template>
  <div class="device-list">
    <div class="page-header">
      <h2>设备管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增设备
      </el-button>
    </div>

    <div class="filter-bar">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input
            v-model="searchForm.deviceName"
            placeholder="设备名称"
            clearable
          />
        </el-col>
        <el-col :span="6">
          <el-select v-model="searchForm.status" placeholder="设备状态" clearable>
            <el-option label="正常运行" value="normal" />
            <el-option label="故障" value="fault" />
            <el-option label="维护中" value="maintenance" />
            <el-option label="已报废" value="scrapped" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </div>

    <div class="table-container">
      <el-table
        :data="tableData"
        border
        stripe
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="deviceId" label="设备编号" width="120" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="deviceType" label="设备类型" width="120" />
        <el-table-column prop="location" label="安装位置" width="180" />
        <el-table-column prop="installDate" label="安装日期" width="120" />
        <el-table-column prop="status" label="设备状态" width="100">
          <template #default="scope">
            <el-tag
              :type="getStatusType(scope.row.status)"
              size="small"
            >
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastMaintenance" label="上次维护" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              :type="scope.row.status === 'normal' ? 'warning' : 'success'"
              size="small"
              @click="handleStatusChange(scope.row)"
            >
              {{ scope.row.status === 'normal' ? '设为故障' : '设为正常' }}
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'

const loading = ref(false)

const searchForm = reactive({
  deviceName: '',
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 假数据
const mockData = [
  {
    deviceId: 'DEV001',
    deviceName: '测斜仪-01号',
    deviceType: '测斜仪',
    location: 'A区-北侧基坑',
    installDate: '2024-01-15',
    status: 'normal',
    lastMaintenance: '2024-03-10'
  },
  {
    deviceId: 'DEV002',
    deviceName: '水位计-02号',
    deviceType: '水位计',
    location: 'B区-南侧基坑',
    installDate: '2024-01-20',
    status: 'fault',
    lastMaintenance: '2024-02-28'
  },
  {
    deviceId: 'DEV003',
    deviceName: '土压力计-01号',
    deviceType: '土压力计',
    location: 'A区-东侧基坑',
    installDate: '2024-02-01',
    status: 'maintenance',
    lastMaintenance: '2024-04-05'
  },
  {
    deviceId: 'DEV004',
    deviceName: '测斜仪-03号',
    deviceType: '测斜仪',
    location: 'C区-西侧基坑',
    installDate: '2024-02-10',
    status: 'normal',
    lastMaintenance: '2024-03-15'
  },
  {
    deviceId: 'DEV005',
    deviceName: '水位计-05号',
    deviceType: '水位计',
    location: 'B区-中央基坑',
    installDate: '2024-02-15',
    status: 'scrapped',
    lastMaintenance: '2024-01-20'
  },
  {
    deviceId: 'DEV006',
    deviceName: '土压力计-02号',
    deviceType: '土压力计',
    location: 'C区-北侧基坑',
    installDate: '2024-03-01',
    status: 'normal',
    lastMaintenance: '2024-04-01'
  }
]

const tableData = ref([])

const getStatusType = (status) => {
  const statusMap = {
    normal: 'success',
    fault: 'danger',
    maintenance: 'warning',
    scrapped: 'info'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    normal: '正常运行',
    fault: '故障',
    maintenance: '维护中',
    scrapped: '已报废'
  }
  return statusMap[status] || '未知'
}

const loadData = () => {
  loading.value = true
  setTimeout(() => {
    // 根据筛选条件过滤数据
    let filteredData = [...mockData]
    
    if (searchForm.deviceName) {
      filteredData = filteredData.filter(item => 
        item.deviceName.toLowerCase().includes(searchForm.deviceName.toLowerCase())
      )
    }
    
    if (searchForm.status) {
      filteredData = filteredData.filter(item => item.status === searchForm.status)
    }
    
    tableData.value = filteredData
    pagination.total = filteredData.length
    loading.value = false
  }, 500)
}

const handleSearch = () => {
  loadData()
  ElMessage.success('查询完成')
}

const handleReset = () => {
  searchForm.deviceName = ''
  searchForm.status = ''
  loadData()
}

const handleAdd = () => {
  ElMessage.info('新增设备功能待开发')
}

const handleEdit = (row) => {
  ElMessage.info(`编辑设备: ${row.deviceName}`)
}

const handleStatusChange = (row) => {
  const newStatus = row.status === 'normal' ? '故障' : '正常'
  ElMessageBox.confirm(
    `确定要将设备 ${row.deviceName} 状态设为 ${newStatus} 吗？`,
    '状态变更确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    row.status = row.status === 'normal' ? 'fault' : 'normal'
    ElMessage.success('状态修改成功')
  }).catch(() => {
    ElMessage.info('已取消操作')
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除设备 ${row.deviceName} 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    const index = tableData.value.findIndex(item => item.deviceId === row.deviceId)
    if (index > -1) {
      tableData.value.splice(index, 1)
      pagination.total = tableData.value.length
      ElMessage.success('删除成功')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

const handleSizeChange = (val) => {
  pagination.pageSize = val
  loadData()
}

const handleCurrentChange = (val) => {
  pagination.currentPage = val
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.device-list {
  background: white;
  border-radius: 8px;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #2c3e50;
}

.filter-bar {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
}

.table-container {
  margin-top: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
