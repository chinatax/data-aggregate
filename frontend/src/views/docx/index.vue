<template>
  <div class="docx-page">
    <!-- Top control bar -->
    <div class="docx-toolbar">
      <div class="toolbar-left">
        <h2>文档编辑器</h2>
        <el-tag v-if="collabActive" type="success" size="small" class="collab-tag">
          <el-icon><Connection /></el-icon>
          {{ peerCount }} 人在线
        </el-tag>
      </div>
      <div class="toolbar-actions">
        <!-- Mode switcher -->
        <el-select v-model="editorMode" size="small" style="width:110px" @change="onModeChange" :disabled="!editorReady">
          <el-option label="编辑" value="editing" />
          <el-option label="修订" value="suggesting" />
          <el-option label="查看" value="viewing" />
        </el-select>
        <!-- Read-only toggle -->
        <el-button size="small" :type="readOnly ? 'warning' : 'default'" @click="readOnly = !readOnly" :disabled="!editorReady">
          {{ readOnly ? '只读中' : '可编辑' }}
        </el-button>
        <!-- Ruler toggle -->
        <el-button size="small" :type="showRuler ? 'primary' : 'default'" plain @click="showRuler = !showRuler" :disabled="!editorReady">
          标尺
        </el-button>
        <!-- Collaboration -->
        <template v-if="!collabActive">
          <el-button :icon="Link" @click="startCollab" :disabled="!editorReady">协同编辑</el-button>
        </template>
        <template v-else>
          <el-button :icon="CopyDocument" @click="copyRoomLink">复制协同链接</el-button>
          <el-button :icon="CloseBold" @click="stopCollab" type="danger" plain>退出协同</el-button>
        </template>
        <!-- File operations -->
        <el-upload v-if="!collabActive" :auto-upload="false" :show-file-list="false" accept=".docx" :on-change="handleFileOpen">
          <el-button :icon="FolderOpened">打开</el-button>
        </el-upload>
        <el-button v-if="!collabActive" :icon="Plus" @click="handleNew">新建</el-button>
        <el-button type="primary" :icon="Download" @click="handleDownload" :disabled="!editorReady">下载</el-button>
      </div>
    </div>

    <!-- Template variables panel -->
    <div v-if="editorReady && templateVars.length" class="template-panel">
      <span class="tp-label">模板变量：</span>
      <el-input
        v-for="v in templateVars" :key="v.tag"
        :model-value="v.value"
        size="small"
        :placeholder="v.tag"
        style="width:140px;margin-right:8px"
        @change="(val: string) => fillTemplate(v.tag, val)"
      >
        <template #prepend>{{ v.tag }}</template>
      </el-input>
      <el-button size="small" @click="refreshTemplateVars">刷新</el-button>
    </div>

    <!-- Editor -->
    <div class="docx-editor-wrapper" v-loading="loading">
      <DocxEditor
        v-if="editorReady && !loading"
        ref="editorRef"
        :document-buffer="collabActive ? undefined : docBuffer"
        :document="collabActive ? emptyDoc : undefined"
        :external-plugins="collabActive ? yPlugins : undefined"
        :external-content="collabActive ? true : undefined"
        :author="collabActive ? userName : undefined"
        :mode="editorMode"
        :read-only="readOnly"
        :show-ruler="showRuler"
        :show-zoom-control="true"
        :document-name="currentFileName"
        :document-name-editable="true"
        :font-families="chineseFonts"
        :i18n="zhCN"
        style="min-height: calc(100vh - 240px);"
      >
        <template #toolbar-extra>
          <button
            class="indent-btn"
            :class="{ active: indentActive }"
            title="首行缩进"
            @click="handleFirstLineIndent"
          >
            首行缩进
          </button>
        </template>
      </DocxEditor>
      <div v-else-if="!loading" class="empty-state">
        <el-empty description="打开 .docx 文件或点击「协同编辑」开始协作">
          <el-button type="primary" @click="handleNew">新建空白文档</el-button>
          <el-button style="margin-left:8px" :icon="Link" @click="startCollabSilent">创建协同文档</el-button>
        </el-empty>
      </div>
      <!-- Back to top -->
      <button v-show="editorReady" class="back-to-top-btn" title="返回顶部" @click="scrollToTop">↑</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { Plus, Download, FolderOpened, Link, Connection, CopyDocument, CloseBold } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { DocxEditor, createEmptyDocument } from '@eigenpal/docx-editor-vue'
import { createEmptyDocx } from '@eigenpal/docx-editor-core/docx/rezip'
import { setIndentFirstLine } from '@eigenpal/docx-editor-core/prosemirror/commands/paragraph'
import type { EditorMode } from '@eigenpal/docx-editor-vue'
import type { FontOption } from '@eigenpal/docx-editor-core/utils/fontOptions'
import '@eigenpal/docx-editor-vue/styles.css'
import * as Y from 'yjs'
import { WebrtcProvider } from 'y-webrtc'
import { ySyncPlugin, yCursorPlugin, yUndoPlugin } from 'y-prosemirror'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

// --- Chinese + default font list ---
const chineseFonts: ReadonlyArray<string | FontOption> = [
  'Arial', 'Calibri', 'Helvetica', 'Verdana', 'Open Sans', 'Roboto',
  'Times New Roman', 'Georgia', 'Cambria', 'Garamond',
  'Courier New', 'Consolas',
  { name: '宋体', fontFamily: 'SimSun, serif', category: 'serif' },
  { name: '黑体', fontFamily: 'SimHei, sans-serif', category: 'sans-serif' },
  { name: '微软雅黑', fontFamily: '"Microsoft YaHei", sans-serif', category: 'sans-serif' },
  { name: '楷体', fontFamily: 'KaiTi, serif', category: 'serif' },
  { name: '仿宋', fontFamily: 'FangSong, serif', category: 'serif' },
  { name: '新宋体', fontFamily: '"NSimSun", serif', category: 'serif' },
  { name: '华文细黑', fontFamily: '"STXihei", sans-serif', category: 'sans-serif' },
  { name: '华文楷体', fontFamily: '"STKaiti", serif', category: 'serif' },
  { name: '华文中宋', fontFamily: '"STZhongsong", serif', category: 'serif' },
  { name: '等线', fontFamily: 'DengXian, sans-serif', category: 'sans-serif' },
  { name: '华文仿宋', fontFamily: '"STFangsong", serif', category: 'serif' },
  { name: '方正舒体', fontFamily: '"FZShuTi", serif', category: 'serif' },
  { name: '方正姚体', fontFamily: '"FZYaoTi", serif', category: 'serif' },
  { name: '方正小标宋', fontFamily: '"FZXiaoBiaoSong-B05S", serif', category: 'serif' },
  { name: '理德小标宋', fontFamily: '"LiDeXiaoBiaoSong", serif', category: 'serif' },
]

const editorRef = ref<InstanceType<typeof DocxEditor> | null>(null)
const docBuffer = ref<ArrayBuffer | null>(null)
const editorReady = ref(false)
const loading = ref(false)
const currentFileName = ref('未命名文档')

// --- Editor config ---
const editorMode = ref<EditorMode>('editing')
const readOnly = ref(false)
const showRuler = ref(true)
const templateVars = ref<{ tag: string; value: string }[]>([])

function onModeChange(_mode: EditorMode) {
  // mode already bound via v-model
}

// --- Chinese i18n ---
const zhCN = {
  toolbar: {
    file: '文件',
    edit: '编辑',
    view: '视图',
    insert: '插入',
    format: '格式',
    table: '表格',
    help: '帮助',
    save: '保存',
    print: '打印',
    undo: '撤销',
    redo: '重做',
    cut: '剪切',
    copy: '复制',
    paste: '粘贴',
    find: '查找',
    replace: '替换',
    bold: '加粗',
    italic: '斜体',
    underline: '下划线',
    strikethrough: '删除线',
    alignLeft: '左对齐',
    alignCenter: '居中',
    alignRight: '右对齐',
    justify: '两端对齐',
    bulletList: '无序列表',
    orderedList: '有序列表',
    indent: '增加缩进',
    outdent: '减少缩进',
    fontSize: '字号',
    fontFamily: '字体',
    textColor: '字体颜色',
    highlightColor: '高亮颜色',
    insertImage: '插入图片',
    insertTable: '插入表格',
    insertLink: '插入链接',
    pageBreak: '分页符',
    header: '页眉',
    footer: '页脚',
    zoom: '缩放',
  }
} as any

// --- Template variables ---
async function refreshTemplateVars() {
  if (!editorRef.value) return
  try {
    const controls = (editorRef.value as any).getContentControls() || []
    templateVars.value = controls
      .filter((c: any) => c.type === 'richText' || c.type === 'text')
      .map((c: any) => ({ tag: c.tag || '', value: '' }))
  } catch { /* ignore */ }
}

async function fillTemplate(tag: string, text: string) {
  if (!editorRef.value) return
  try {
    (editorRef.value as any).setContentControlContent({ tag }, text)
    ElMessage.success(`已填入「${tag}」`)
  } catch {
    ElMessage.error('填充失败')
  }
}

// --- Collaboration state ---
const collabActive = ref(false)
const peerCount = ref(0)
let ydoc: Y.Doc | null = null
let provider: WebrtcProvider | null = null
const yPlugins = ref<any[]>([])
const emptyDoc = createEmptyDocument()
const roomName = ref('')
const userName = computed(() => auth.username || '匿名用户')

function generateRoomName(): string {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789'
  let room = ''
  for (let i = 0; i < 10; i++) room += chars[Math.floor(Math.random() * chars.length)]
  return `docx-room-${room}`
}

function startCollabSilent() {
  editorReady.value = true
  startCollab()
}

function startCollab() {
  if (collabActive.value) return
  if (!auth.isLoggedIn) { ElMessage.warning('请先登录后再使用协同编辑'); return }
  roomName.value = generateRoomName()
  ydoc = new Y.Doc()
  provider = new WebrtcProvider(roomName.value, ydoc, {
    signaling: ['wss://signaling.yjs.dev', 'wss://y-webrtc-signaling-eu.herokuapp.com', 'wss://y-webrtc-signaling-us.herokuapp.com'],
  })
  const fragment = ydoc.getXmlFragment('prosemirror')
  yPlugins.value = [ySyncPlugin(fragment), yCursorPlugin(provider.awareness), yUndoPlugin()]
  provider.awareness.setLocalStateField('user', {
    name: userName.value,
    color: '#' + Math.floor(Math.random() * 16777215).toString(16).padStart(6, '0'),
  })
  provider.awareness.on('change', () => { peerCount.value = provider?.awareness.getStates().size ?? 0 })
  collabActive.value = true
  if (!editorReady.value) editorReady.value = true
  ElMessage.success('协同编辑已开启，分享链接即可邀请他人')
}

function stopCollab() {
  provider?.destroy(); ydoc?.destroy(); provider = null; ydoc = null
  yPlugins.value = []; collabActive.value = false; peerCount.value = 0; editorReady.value = false
  ElMessage.info('已退出协同编辑')
}

async function copyRoomLink() {
  const url = `${window.location.origin}${window.location.pathname}?room=${roomName.value}`
  try { await navigator.clipboard.writeText(url); ElMessage.success('协同链接已复制，发给他人即可加入编辑') }
  catch { ElMessage.warning('复制失败，请手动复制地址栏链接') }
}

// --- File operations ---
async function handleFileOpen(file: any) {
  loading.value = true
  try {
    docBuffer.value = await file.raw.arrayBuffer()
    currentFileName.value = file.name.replace('.docx', '')
    editorReady.value = true
    setTimeout(refreshTemplateVars, 2000)
  } catch { ElMessage.error('文件读取失败') }
  finally { loading.value = false }
}

async function handleNew() {
  docBuffer.value = await createEmptyDocx()
  currentFileName.value = '未命名文档'
  editorReady.value = true
}

async function handleDownload() {
  if (!editorRef.value) return
  try {
    const buffer = await (editorRef.value as any).save()
    const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url
    a.download = `${currentFileName.value || 'document'}.docx`; a.click()
    URL.revokeObjectURL(url); ElMessage.success('文档已下载')
  } catch { ElMessage.error('保存失败') }
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// --- First-line indent ---
const FIRST_LINE_INDENT_TWIPS = 480 // ~2 Chinese characters at 12pt
const indentActive = ref(false)

function handleFirstLineIndent() {
  const view = (editorRef.value as any)?.getEditorRef?.()?.getView?.()
  if (!view) return
  if (indentActive.value) {
    setIndentFirstLine(0)(view.state, view.dispatch)
    indentActive.value = false
  } else {
    setIndentFirstLine(FIRST_LINE_INDENT_TWIPS)(view.state, view.dispatch)
    indentActive.value = true
  }
}

// --- Auto-join collaboration from URL ---
if (auth.isLoggedIn) {
  const params = new URLSearchParams(window.location.search)
  const roomFromUrl = params.get('room')
  if (roomFromUrl) {
    roomName.value = roomFromUrl; editorReady.value = true
    setTimeout(() => {
      ydoc = new Y.Doc()
      provider = new WebrtcProvider(roomName.value, ydoc, {
        signaling: ['wss://signaling.yjs.dev', 'wss://y-webrtc-signaling-eu.herokuapp.com', 'wss://y-webrtc-signaling-us.herokuapp.com'],
      })
      const fragment = ydoc.getXmlFragment('prosemirror')
      yPlugins.value = [ySyncPlugin(fragment), yCursorPlugin(provider.awareness), yUndoPlugin()]
      provider.awareness.setLocalStateField('user', { name: userName.value, color: '#' + Math.floor(Math.random() * 16777215).toString(16).padStart(6, '0') })
      provider.awareness.on('change', () => { peerCount.value = provider?.awareness.getStates().size ?? 0 })
      collabActive.value = true
    }, 100)
  }
}

onUnmounted(() => { stopCollab() })
</script>

<style scoped>
.docx-page { padding: 20px; max-width: 100%; margin: 0 auto; }
.docx-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid #eee; flex-wrap: wrap; gap: 8px; }
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.toolbar-left h2 { margin: 0; font-size: 20px; white-space: nowrap; }
.collab-tag { display: flex; align-items: center; gap: 4px; }
.toolbar-actions { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.template-panel { display: flex; align-items: center; gap: 4px; margin-bottom: 8px; padding: 6px 10px; background: #f5f7fa; border-radius: 6px; flex-wrap: wrap; }
.tp-label { font-size: 13px; color: #666; white-space: nowrap; }
.docx-editor-wrapper { background: #fff; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); min-height: 60vh; position: relative; }
.empty-state { display: flex; align-items: center; justify-content: center; min-height: 60vh; }
.indent-btn { padding: 4px 10px; border: 1px solid #dcdfe6; border-radius: 4px; background: #fff; color: #606266; cursor: pointer; font-size: 13px; white-space: nowrap; }
.indent-btn:hover { border-color: #409eff; color: #409eff; }
.indent-btn.active { background: #ecf5ff; border-color: #409eff; color: #409eff; }
.back-to-top-btn { position: sticky; bottom: 24px; float: right; width: 40px; height: 40px; border: 1px solid #dcdfe6; border-radius: 50%; background: #fff; color: #606266; cursor: pointer; font-size: 18px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); z-index: 10; display: flex; align-items: center; justify-content: center; right: calc((100% - 816px) / 2 - 70px); }
.back-to-top-btn:hover { border-color: #409eff; color: #409eff; box-shadow: 0 4px 12px rgba(64,158,255,0.25); }
</style>
