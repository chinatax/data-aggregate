<template>
  <div class="page-container">
    <div class="section-title">股票数据</div>
    <el-empty :description="siteStore.modulePlaceholder">
      <template #image>
        <el-icon :size="60" color="#BDBDBD"><TrendCharts /></el-icon>
      </template>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { TrendCharts } from '@element-plus/icons-vue'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()
</script>
