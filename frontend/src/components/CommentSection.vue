<template>
  <div class="comment-section">
    <h3 class="comment-title">留言 ({{ comments.length }})</h3>

    <!-- 留言表单 -->
    <div class="comment-form">
      <div class="form-row" v-if="!authStore.isLoggedIn">
        <el-input
          v-model="form.authorName"
          placeholder="昵称（选填，默认「匿名用户」）"
          maxlength="20"
          size="default"
          class="name-input"
        />
        <span class="char-count">{{ form.authorName.length }}/20</span>
      </div>
      <div class="form-row" v-else>
        <span class="login-badge">
          <el-icon><User /></el-icon>
          {{ authStore.displayName }}
        </span>
      </div>
      <el-input
        v-model="form.content"
        type="textarea"
        :rows="3"
        placeholder="写下你的想法..."
        maxlength="2000"
        show-word-limit
      />
      <div class="form-footer">
        <span class="form-tip">支持匿名留言，文明讨论</span>
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmit">
          发表留言
        </el-button>
      </div>
    </div>

    <!-- 留言列表 -->
    <div v-if="comments.length > 0" class="comment-list">
      <template v-for="comment in comments" :key="comment.id">
        <div class="comment-item">
          <div class="comment-avatar" :style="{ background: avatarColor(comment.authorName) }">
            {{ comment.authorName.charAt(0) }}
          </div>
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-name">{{ comment.authorName }}</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-actions">
              <span
                class="reply-btn"
                :class="{ active: replyTarget === comment.id }"
                @click="toggleReply(comment.id)"
              >
                <el-icon><ChatLineSquare /></el-icon> 回复
              </span>
            </div>

            <!-- 回复表单 -->
            <div v-if="replyTarget === comment.id" class="reply-form">
              <el-input
                v-if="!authStore.isLoggedIn"
                v-model="replyForm.authorName"
                placeholder="昵称（选填）"
                maxlength="20"
                size="small"
                class="reply-name-input"
              />
              <span v-else class="login-badge small">
                <el-icon><User /></el-icon>
                {{ authStore.displayName }}
              </span>
              <el-input
                v-model="replyForm.content"
                type="textarea"
                :rows="2"
                placeholder="回复 {{ comment.authorName }}..."
                maxlength="2000"
                size="small"
              />
              <div class="reply-form-actions">
                <el-button size="small" text @click="cancelReply">取消</el-button>
                <el-button size="small" type="primary" :loading="replying" @click="handleReply(comment.id)">
                  回复
                </el-button>
              </div>
            </div>

            <!-- 子回复列表 -->
            <div v-if="comment.children && comment.children.length > 0" class="children-list">
              <div v-for="child in comment.children" :key="child.id" class="child-item">
                <div class="comment-avatar small" :style="{ background: avatarColor(child.authorName) }">
                  {{ child.authorName.charAt(0) }}
                </div>
                <div class="comment-body">
                  <div class="comment-header">
                    <span class="comment-name">{{ child.authorName }}</span>
                    <span v-if="child.parentId && child.parentId !== comment.id" class="reply-to">
                      回复 @{{ getParentName(child.parentId) }}
                    </span>
                    <span class="comment-time">{{ formatTime(child.createTime) }}</span>
                  </div>
                  <div class="comment-content">{{ child.content }}</div>
                  <div class="comment-actions">
                    <span
                      class="reply-btn"
                      :class="{ active: replyTarget === child.id }"
                      @click="toggleReply(child.id)"
                    >
                      <el-icon><ChatLineSquare /></el-icon> 回复
                    </span>
                  </div>

                  <!-- 子回复的回复表单 -->
                  <div v-if="replyTarget === child.id" class="reply-form">
                    <el-input
                      v-model="replyForm.authorName"
                      placeholder="昵称（选填）"
                      maxlength="20"
                      size="small"
                      class="reply-name-input"
                    />
                    <el-input
                      v-model="replyForm.content"
                      type="textarea"
                      :rows="2"
                      :placeholder="`回复 ${child.authorName}...`"
                      maxlength="2000"
                      size="small"
                    />
                    <div class="reply-form-actions">
                      <el-button size="small" text @click="cancelReply">取消</el-button>
                      <el-button size="small" type="primary" :loading="replying" @click="handleReply(child.id)">
                        回复
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <div v-else class="comment-empty">
      <p>暂无留言，来抢沙发吧～</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ChatLineSquare, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getComments, addComment, addReply, type CommentItem } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  blogId: number
}>()

const authStore = useAuthStore()

const comments = ref<CommentItem[]>([])
const replyTarget = ref<number | null>(null)
const submitting = ref(false)
const replying = ref(false)

const form = reactive({ authorName: '', content: '' })
const replyForm = reactive({ authorName: '', content: '' })

const avatarColors = [
  '#6366F1', '#8B5CF6', '#EC4899', '#F43F5E', '#F97316',
  '#EAB308', '#22C55E', '#14B8A6', '#06B6D4', '#3B82F6',
]

function avatarColor(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return avatarColors[Math.abs(hash) % avatarColors.length]
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function flatComments(list: CommentItem[]): CommentItem[] {
  const result: CommentItem[] = []
  for (const item of list) {
    result.push(item)
    if (item.children && item.children.length > 0) {
      result.push(...flatComments(item.children))
    }
  }
  return result
}

function getParentName(parentId: number): string {
  const all = flatComments(comments.value)
  const p = all.find(c => c.id === parentId)
  return p ? p.authorName : ''
}

async function loadComments() {
  try {
    comments.value = await getComments(props.blogId)
  } catch {
    // error handled by interceptor
  }
}

function toggleReply(commentId: number) {
  if (replyTarget.value === commentId) {
    replyTarget.value = null
    replyForm.authorName = ''
    replyForm.content = ''
  } else {
    replyTarget.value = commentId
    replyForm.authorName = ''
    replyForm.content = ''
  }
}

function cancelReply() {
  replyTarget.value = null
  replyForm.authorName = ''
  replyForm.content = ''
}

async function handleSubmit() {
  if (!form.content.trim()) {
    ElMessage.warning('请输入留言内容')
    return
  }
  submitting.value = true
  try {
    await addComment(props.blogId, {
      authorName: authStore.isLoggedIn ? authStore.displayName : (form.authorName.trim() || '匿名用户'),
      content: form.content.trim(),
    })
    ElMessage.success('留言成功')
    form.authorName = ''
    form.content = ''
    await loadComments()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleReply(commentId: number) {
  if (!replyForm.content.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await addReply(commentId, {
      authorName: authStore.isLoggedIn ? authStore.displayName : (replyForm.authorName.trim() || '匿名用户'),
      content: replyForm.content.trim(),
    })
    ElMessage.success('回复成功')
    cancelReply()
    await loadComments()
  } catch {
    // error handled by interceptor
  } finally {
    replying.value = false
  }
}

watch(() => props.blogId, () => {
  if (props.blogId) loadComments()
}, { immediate: true })
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.comment-section {
  margin-top: 48px;
  padding-top: 32px;
  border-top: 1px solid $border;
}

.comment-title {
  font-size: 18px;
  font-weight: 600;
  color: $text-main;
  margin-bottom: 20px;
}

/* ====== 留言表单 ====== */
.comment-form {
  margin-bottom: 32px;

  .form-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;

    .name-input {
      width: 220px;
    }

    .char-count {
      font-size: 12px;
      color: $gray-300;
    }

    .login-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      background: #EFF6FF;
      border: 1px solid #BFDBFE;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 500;
      color: #2563EB;

      &.small {
        padding: 4px 10px;
        font-size: 12px;
      }
    }
  }

  .form-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 10px;

    .form-tip {
      font-size: 13px;
      color: $gray-400;
    }
  }
}

/* ====== 留言列表 ====== */
.comment-list {
  .comment-item {
    display: flex;
    gap: 12px;
    padding: 16px 0;
    border-bottom: 1px solid $border;

    &:last-child {
      border-bottom: none;
    }
  }
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  flex-shrink: 0;
  user-select: none;

  &.small {
    width: 32px;
    height: 32px;
    font-size: 13px;
  }
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;

  .comment-name {
    font-size: 14px;
    font-weight: 500;
    color: $text-main;
  }

  .reply-to {
    font-size: 12px;
    color: #38BDF8;
  }

  .comment-time {
    font-size: 12px;
    color: $gray-400;
  }
}

.comment-content {
  font-size: 14px;
  line-height: 1.7;
  color: $text-sub;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  margin-top: 8px;

  .reply-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: $gray-400;
    cursor: pointer;
    transition: color 0.2s;

    &:hover {
      color: #38BDF8;
    }

    &.active {
      color: #38BDF8;
    }
  }
}

/* ====== 回复表单 ====== */
.reply-form {
  margin-top: 12px;
  padding: 12px;
  background: $gray-50;
  border-radius: $radius;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .reply-name-input {
    width: 200px;
  }

  .reply-form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

/* ====== 子回复 ====== */
.children-list {
  margin-top: 12px;
  padding-left: 24px;
  border-left: 2px solid $gray-100;

  .child-item {
    display: flex;
    gap: 10px;
    padding: 10px 0;

    &:first-child {
      padding-top: 0;
    }

    &:last-child {
      padding-bottom: 0;
    }
  }
}

/* ====== 空状态 ====== */
.comment-empty {
  text-align: center;
  padding: 40px 0;
  color: $gray-400;
  font-size: 14px;
}
</style>
