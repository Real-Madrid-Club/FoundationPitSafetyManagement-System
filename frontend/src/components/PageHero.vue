<template>
<header class="page-hero">
  <DesignArt v-if="!breadcrumb" kind="banner" class="hero-art" />
  <div class="hero-copy">
    <h1>{{ title }}</h1>
    <p>{{ description }}</p>
  </div>
  <div v-if="breadcrumb" class="hero-breadcrumb">
    <router-link to="/dashboard">⌂ 首页</router-link>
    <span>/</span>
    {{ title }}
  </div>
</header>
</template>

<script setup>
defineProps({ title: String, description: String, breadcrumb: Boolean })
</script>
