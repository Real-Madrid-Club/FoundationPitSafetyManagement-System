<template>
<svg
  class="design-art"
  :class="`art-${kind}`"
  :viewBox="region.box"
  preserveAspectRatio="xMidYMid slice"
  fill="none"
  aria-hidden="true"
  focusable="false"
>
  <image :href="`/design/${region.file}.png`" :width="region.width" :height="region.height" />
</svg>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ kind: { type: String, default: 'banner' } })
// Use only the decorative regions of the supplied references. Text and controls are live HTML.
const regions = {
  sidebar: { file: 'dashboard', box: '0 555 287 386', width: 1672, height: 941 },
  banner: { file: 'dashboard', box: '820 60 852 136', width: 1672, height: 941 },
  robot: { file: 'assistant', box: '746 290 447 220', width: 1672, height: 941 },
  empty: { file: 'workorder', box: '770 465 450 187', width: 1672, height: 941 },
  'login-left': { file: 'login', box: '0 180 400 906', width: 1448, height: 1086 },
  'login-right': { file: 'login', box: '1048 180 400 906', width: 1448, height: 1086 },
}
const region = computed(() => regions[props.kind] || regions.banner)
</script>

<style scoped>
.design-art {
  display: block;
  overflow: hidden;
  pointer-events: none;
  width: 100%;
  height: auto;
}
</style>
