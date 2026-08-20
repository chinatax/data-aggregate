import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getSiteConfig } from '@/api/settings'

export interface Channel {
  path: string
  name: string
  icon: string
}

const defaults: Record<string, string> = {
  site_name: '数据聚合智能AI',
  site_subtitle: '数据聚合智能应用',
  login_slogan: '分享技术见解，记录成长点滴',
  login_slogan_color: '#ffffff',
  register_slogan: '加入我们，开始创作',
  register_slogan_color: '#ffffff',
  home_hero_description: '汇聚股票、电子书集、行政区划等多维度数据，提供一站式数据查询与洞察服务',
  home_hero_description_color: '#ffffff',
  blog_slogan: '分享技术见解，记录成长点滴',
  stock_desc: '实时行情、历史走势、智能分析',
  docx_desc: '在线文档编辑、格式兼容Word、协同办公',
  ebook_desc: '海量图书、分类检索、智能推荐',
  district_desc: '全国省市区县、层级关联、数据查询',
  module_placeholder: '模块开发中，敬请期待...',
  channels: '[{"path":"/","name":"首页概览","icon":"HomeFilled"},{"path":"/blog","name":"博客","icon":"Document"},{"path":"/docx","name":"文档编辑","icon":"Edit"},{"path":"/ebook","name":"电子书集","icon":"Reading"},{"path":"/district","name":"行政区划","icon":"MapLocation"},{"path":"/contact","name":"联系我们","icon":"Service"}]',
  system_version: 'v1.0.0',
  author_bio: '技术博主 · 分享干货',
  copyright_text: '',
  email_subject_prefix: '数据聚合',
  upload_allowed_extensions: 'jpg,jpeg,png,gif,webp,bmp,svg',
}

export const useSiteStore = defineStore('site', () => {
  const config = ref<Record<string, string>>({})
  const loaded = ref(false)

  function get(key: string): string {
    return config.value[key] || defaults[key] || ''
  }

  function getColor(key: string): string {
    return get(key) || '#ffffff'
  }

  const siteName = computed(() => get('site_name'))
  const siteSubtitle = computed(() => get('site_subtitle'))
  const loginSlogan = computed(() => get('login_slogan'))
  const loginSloganColor = computed(() => getColor('login_slogan_color'))
  const registerSlogan = computed(() => get('register_slogan'))
  const registerSloganColor = computed(() => getColor('register_slogan_color'))
  const homeHeroDesc = computed(() => get('home_hero_description'))
  const homeHeroDescColor = computed(() => getColor('home_hero_description_color'))
  const blogSlogan = computed(() => get('blog_slogan'))
  const stockDesc = computed(() => get('stock_desc'))
  const ebookDesc = computed(() => get('ebook_desc'))
  const districtDesc = computed(() => get('district_desc'))
  const modulePlaceholder = computed(() => get('module_placeholder'))
  const systemVersion = computed(() => get('system_version'))
  const authorBio = computed(() => get('author_bio'))

  const channelList = computed<Channel[]>(() => {
    try {
      return JSON.parse(get('channels'))
    } catch {
      return JSON.parse(defaults['channels'])
    }
  })

  function saveChannels(list: Channel[]) {
    config.value = { ...config.value, channels: JSON.stringify(list) }
  }

  async function fetchConfig() {
    try {
      const data = await getSiteConfig()
      config.value = data
    } catch {
      // 使用默认值
    } finally {
      loaded.value = true
    }
  }

  async function refreshConfig() {
    await fetchConfig()
  }

  function applyConfig(updates: Record<string, string>) {
    config.value = { ...config.value, ...updates }
  }

  function clearConfig() {
    config.value = {}
    loaded.value = false
  }

  return {
    config, loaded,
    get, getColor,
    siteName, siteSubtitle,
    loginSlogan, loginSloganColor,
    registerSlogan, registerSloganColor,
    homeHeroDesc, homeHeroDescColor,
    blogSlogan,
    stockDesc, ebookDesc, districtDesc,
    modulePlaceholder,
    systemVersion, authorBio,
    channelList, saveChannels,
    fetchConfig, refreshConfig, applyConfig, clearConfig,
  }
})
