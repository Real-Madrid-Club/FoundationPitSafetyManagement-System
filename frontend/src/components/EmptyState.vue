<template>
<div class="empty-state" :class="{ compact }">
  <DesignArt kind="empty" />
  <h3>{{ title }}</h3>
  <p v-if="description">{{ description }}</p>
  <slot />
</div>
</template>

<script setup>
defineProps({ title: { type: String, default: '暂无数据' }, description: String, compact: Boolean })
</script>
