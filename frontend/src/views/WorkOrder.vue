<template>
  <div class="workorder">
    <div class="page-header">
      <h2>工单调度</h2>
      <el-button type="primary" @click="handleCreateWorkOrder">
        <el-icon><Plus /></el-icon>
        创建工单
      </el-button>
    </div>

    <div class="filter-bar">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="searchForm.status" placeholder="工单状态" clearable>
            <el-option label="待派单" value="pending" />
            <el-option label="已派单" value="assigned" />
            <el-option label="进行中" value="in_progress" />
            <el-option label="已完成" value="completed" />
            <el-option label="已验收" value="accepted" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="searchForm.priority" placeholder="优先级" clearable>
            <el-option label="紧急" value="urgent" />
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
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
        <el-table-column prop="workOrderId" label="工单编号" width="120" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="faultType" label="故障类型" width="120" />
        <el-table-column prop="reporter" label="报修人" width="100" />
        <el-table-column prop="reportTime" label="报修时间" width="150" />
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="scope">
            <el-tag
              :type="getPriorityType(scope.row.priority)"
              size="small"
            >
              {{ getPriorityText(scope.row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag
              :type="getStatusType(scope.row.status)"
              size="small"
            >
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignee" label="维修人员" width="100" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="handleView(scope.row)"
            >
              查看
            </el-button>
            <el-button
              v-if="scope.row.status === 'pending'"
              type="success"
              size="small"
              @click="handleAssign(scope.row)"
            >
              派单
            </el-button>
            <el-button
              v-if="scope.row.status === 'completed'"
              type="warning"
              size="small"
              @click="handleAccept(scope.row)"
            >
              验收
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
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'

const loading = ref(false)

const searchForm = reactive({
  status: '',
  priority: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 假数据
const mockData = [
  {
    workOrderId: 'WO001',
    deviceName: '测斜仪-01号',
    faultType: '数据异常',
    reporter: '张工',
    reportTime: '2024-05-06 09:30',
    priority: 'urgent',
    status: 'pending',
    assignee: ''
  },
  {
    workOrderId: 'WO002',
    deviceName: '水位计-03号',
    faultType: '设备离线',
    reporter: '李工',
    reportTime: '2024-05-06 08:15',
    priority: 'high',
    status: 'assigned',
    assignee: '王师傅'
  },
  {
    workOrderId: 'WO003',
    deviceName: '土压力计-02号',
    faultType: '需要校准',
    reporter: '赵工',
    reportTime: '2024-05-05 16:45',
    priority: 'medium',
    status: 'in_progress',
    assignee: '刘师傅'
  },
  {
    workOrderId: 'WO004',
    deviceName: '测斜仪-02号',
    faultType: '电源故障',
    reporter: '陈工',
    reportTime: '2024-05-05 14:20',
    priority: 'low',
    status: 'completed',
    assignee: '孙师傅'
  },
  {
    workOrderId: 'WO005',
    deviceName: '水位计-01号',
    faultType: '传感器异常',
    reporter: '周工',
    reportTime: '2024-05-05 10:30',
    priority: 'medium',
    status: 'accepted',
    assignee: '钱师傅'
  }
]

const tableData = ref([])

const getPriorityType = (priority) => {
  const priorityMap = {
    urgent: 'danger',
    high: 'warning',
    medium: 'primary',
    low: 'info'
  }
  return priorityMap[priority] || 'info'
}

const getPriorityText = (priority) => {
  const priorityMap = {
    urgent: '紧急',
    high: '高',
    medium: '中',
    low: '低'
  }
  return priorityMap[priority] || '未知'
}

const getStatusType = (status) => {
  const statusMap = {
    pending: 'info',
    assigned: 'primary',
    in_progress: 'warning',
    completed: 'success',
    accepted: 'success'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    pending: '待派单',
    assigned: '已派单',
    in_progress: '进行中',
    completed: '已完成',
    accepted: '已验收'
  }
  return statusMap[status] || '未知'
}

const loadData = () => {
  loading.value = true
  setTimeout(() => {
    // 根据筛选条件过滤数据
    let filteredData = [...mockData]
    
    if (searchForm.status) {
      filteredData = filteredData.filter(item => item.status === searchForm.status)
    }
    
    if (searchForm.priority) {
      filteredData = filteredData.filter(item => item.priority === searchForm.priority)
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
  searchForm.status = ''
  searchForm.priority = ''
  loadData()
}

const handleCreateWorkOrder = () => {
  ElMessage.info('创建工单功能待开发')
}

const handleView = (row) => {
  ElMessage.info(`查看工单: ${row.workOrderId}`)
}

const handleAssign = (row) => {
  ElMessage.success(`工单 ${row.workOrderId} 已派单`)
  row.status = 'assigned'
  row.assignee = '待分配'
}

const handleAccept = (row) => {
  ElMessage.success(`工单 ${row.workOrderId} 已验收`)
  row.status = 'accepted'
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
.workorder {
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
