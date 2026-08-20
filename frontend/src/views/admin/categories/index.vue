<template>
  <div class="categories-page">
    <div class="page-header">
      <h2>分类管理</h2>
      <el-button type="primary" @click="openAdd">
        <el-icon><Plus /></el-icon>
        新增分类
      </el-button>
    </div>

    <el-table :data="list" border stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="code" label="分类标识" width="160">
        <template #default="{ row }">
          {{ row.code }}
          <el-tag v-if="row.system" size="small" type="info" style="margin-left:6px">系统</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="显示名称" />
      <el-table-column label="操作" width="300">
        <template #default="{ row, $index }">
          <el-button link type="primary" :disabled="$index === 0" @click="handleMoveUp(row)">
            <el-icon><Top /></el-icon>
            向上
          </el-button>
          <el-button link type="primary" :disabled="$index === list.length - 1" @click="handleMoveDown(row)">
            <el-icon><Bottom /></el-icon>
            向下
          </el-button>
          <el-button link type="primary" :disabled="row.system" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :disabled="row.system" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="420px"
      align-center
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="分类标识">
          <el-input
            v-model="form.code"
            placeholder="大写字母、数字、下划线"
            :disabled="isEdit"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="form.name" placeholder="如：前端" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Top, Bottom } from '@element-plus/icons-vue'
import { getCategories, createCategory, updateCategory, deleteCategory, moveCategoryUp, moveCategoryDown } from '@/api/category'
import type { CategoryItem } from '@/api/category'

interface CategoryRow extends CategoryItem {
  system?: boolean
}
const list = ref<CategoryRow[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(0)

const form = ref({ code: '', name: '' })

async function load() {
  loading.value = true
  try {
    list.value = await getCategories() as any[]
  } finally {
    loading.value = false
  }
}

function openAdd() {
  isEdit.value = false
  editId.value = 0
  form.value = { code: '', name: '' }
  dialogVisible.value = true
}

function openEdit(row: CategoryRow) {
  if (row.system) return
  isEdit.value = true
  editId.value = row.id
  form.value = { code: row.code, name: row.name }
  dialogVisible.value = true
}

function invalidateBlogCache() {
  sessionStorage.removeItem('blog_categories')
}

async function handleSubmit() {
  const { code, name } = form.value
  if (!code.trim()) { ElMessage.warning('分类标识不能为空'); return }
  if (!name.trim()) { ElMessage.warning('分类名称不能为空'); return }
  if (!/^[A-Z0-9_]+$/.test(code.trim().toUpperCase())) {
    ElMessage.warning('分类标识仅支持大写字母、数字、下划线')
    return
  }
  try {
    if (isEdit.value) {
      await updateCategory(editId.value, { name: name.trim() })
    } else {
      await createCategory({ code: code.trim().toUpperCase(), name: name.trim() })
    }
    dialogVisible.value = false
    invalidateBlogCache()
    await load()
  } catch (e: any) {
    if (e?.message && e.message !== 'Error') {
      ElMessage.error(e.message)
    }
  }
}

async function handleMoveUp(row: CategoryRow) {
  try {
    await moveCategoryUp(row.id)
    invalidateBlogCache()
    await load()
  } catch (e: any) {
    if (e?.message && e.message !== 'Error') {
      ElMessage.error(e.message)
    }
  }
}

async function handleMoveDown(row: CategoryRow) {
  try {
    await moveCategoryDown(row.id)
    invalidateBlogCache()
    await load()
  } catch (e: any) {
    if (e?.message && e.message !== 'Error') {
      ElMessage.error(e.message)
    }
  }
}

async function handleDelete(row: CategoryRow) {
  if (row.system) return
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」分类？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteCategory(row.id)
    invalidateBlogCache()
    await load()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message && e.message !== 'Error') {
      ElMessage.error(e.message)
    }
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped lang="scss">
.categories-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    position: relative;
    padding-left: 16px;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 4px;
      bottom: 4px;
      width: 4px;
      background: #e0e0e0;
      border-radius: 2px;
    }
  }
}
</style>
