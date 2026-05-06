<template>
  <div id="app">
    <el-container class="layout-container">
      <el-header class="layout-header">
        <h2>基坑安全管理系统</h2>
        <el-menu
          mode="horizontal"
          :default-active="activeMenu"
          router
          class="header-menu"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/ai-chat">AI 助手</el-menu-item>
        </el-menu>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style>
body {
  margin: 0;
  padding: 0;
  background: #f5f7fa;
}
.layout-container {
  min-height: 100vh;
}
.layout-header {
  background: #fff;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
}
.layout-header h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
  margin-right: 40px;
}
.header-menu {
  border-bottom: none !important;
  flex: 1;
}
</style>
