<template>
  <el-dialog v-model="visible" title="修改密码" width="400px" :close-on-click-modal="false" @closed="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入旧密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password placeholder="至少6位" />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { changePasswordApi } from '@/api/auth'

const visible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (_rule: any, value: string, callback: (err?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

function resetForm() {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await changePasswordApi({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    visible.value = false
  } catch {
    // error message handled by interceptor
  } finally {
    submitting.value = false
  }
}

function open() {
  visible.value = true
}

defineExpose({ open })
</script>
