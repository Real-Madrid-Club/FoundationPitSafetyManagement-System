<template>
  <div class="home-container">
    <el-card>
      <h3>欢迎使用基坑安全管理系统</h3>
      <p>本系统提供基坑设备维保与智能工单调度功能，集成 DeepSeek AI 助手辅助故障诊断与健康监测。</p>
      <el-button type="primary" @click="$router.push('/ai-chat')">进入 AI 助手</el-button>
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-container {
  max-width: 800px;
  margin: 40px auto;
}
</style>
