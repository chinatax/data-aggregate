<template>
  <div class="contact-page">
    <!-- Hero -->
    <div class="contact-hero">
      <div class="hero-content">
        <h1>联系我们</h1>
        <p>如果觉得本站对你有帮助，欢迎请作者喝杯咖啡 ☕️</p>
      </div>
      <div class="hero-illustration">
        <svg viewBox="0 0 400 180" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect x="60" y="40" width="120" height="100" rx="12" fill="#FAFAFA" stroke="#E0E0E0" stroke-width="2"/>
          <rect x="70" y="55" width="100" height="8" rx="4" fill="#F0F0F0"/>
          <rect x="70" y="72" width="70" height="6" rx="3" fill="#F0F0F0"/>
          <rect x="70" y="86" width="80" height="6" rx="3" fill="#F0F0F0"/>
          <circle cx="120" cy="120" r="12" fill="#F5F5F5" stroke="#E0E0E0" stroke-width="1.5"/>
          <rect x="210" y="65" width="130" height="50" rx="10" fill="#38BDF8" opacity="0.08" stroke="#38BDF8" stroke-width="1.5"/>
          <path d="M245 90L260 75L270 82L280 72L290 78" stroke="#38BDF8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M245 90L245 78" stroke="#38BDF8" stroke-width="1.5" stroke-linecap="round"/>
          <rect x="210" y="15" width="80" height="30" rx="6" fill="#FAFAFA" stroke="#EEEEEE" stroke-width="1"/>
          <rect x="220" y="22" width="30" height="6" rx="3" fill="#E0E0E0"/>
          <rect x="220" y="32" width="40" height="6" rx="3" fill="#E0E0E0"/>
          <circle cx="70" cy="160" r="4" fill="#38BDF8" opacity="0.4"/>
          <circle cx="310" cy="40" r="3" fill="#38BDF8" opacity="0.3"/>
          <circle cx="340" cy="95" r="5" fill="#38BDF8" opacity="0.25"/>
        </svg>
      </div>
    </div>

    <!-- 捐赠方式 -->
    <div class="section-title">捐赠支持</div>
    <p class="section-subtitle">每一份支持都是我们持续改进的动力</p>

    <div class="donate-grid">
      <!-- 支付宝 -->
      <div class="donate-card">
        <div class="card-header">
          <svg class="pay-icon alipay" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="40" height="40" rx="10" fill="#1677FF"/>
            <text x="20" y="27" text-anchor="middle" fill="white" font-size="20" font-weight="bold">支</text>
          </svg>
          <div>
            <h3>支付宝</h3>
            <span>扫一扫，轻松捐赠</span>
          </div>
        </div>
        <div class="qr-wrap">
          <div class="qr-box">
            <img
              v-if="alipayQr"
              :src="alipayQr"
              alt="支付宝收款码"
              class="qr-img"
            />
            <div v-else class="qr-placeholder">
              <svg viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="20" y="20" width="160" height="160" rx="8" fill="#F5F5F5" stroke="#E0E0E0" stroke-width="2"/>
                <rect x="40" y="40" width="40" height="40" rx="4" fill="#E8E8E8"/>
                <rect x="100" y="40" width="40" height="40" rx="4" fill="#E8E8E8"/>
                <rect x="160" y="40" width="0" height="0" fill="none"/>
                <rect x="40" y="100" width="40" height="40" rx="4" fill="#E8E8E8"/>
                <rect x="100" y="100" width="40" height="40" rx="4" fill="#E8E8E8"/>
                <rect x="160" y="100" width="0" height="0" fill="none"/>
                <rect x="160" y="160" width="0" height="0" fill="none"/>
                <rect x="40" y="160" width="40" height="0" fill="none"/>
                <text x="100" y="110" text-anchor="middle" fill="#BDBDBD" font-size="13">请上传</text>
                <text x="100" y="130" text-anchor="middle" fill="#BDBDBD" font-size="13">收款码图片</text>
              </svg>
            </div>
          </div>
          <p class="qr-tip">打开支付宝「扫一扫」即可捐赠</p>
        </div>
      </div>

      <!-- 微信支付 -->
      <div class="donate-card">
        <div class="card-header">
          <svg class="pay-icon wechat" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="40" height="40" rx="10" fill="#07C160"/>
            <text x="20" y="27" text-anchor="middle" fill="white" font-size="20" font-weight="bold">微</text>
          </svg>
          <div>
            <h3>微信支付</h3>
            <span>扫一扫，支持作者</span>
          </div>
        </div>
        <div class="qr-wrap">
          <div class="qr-box">
            <img
              v-if="wechatQr"
              :src="wechatQr"
              alt="微信收款码"
              class="qr-img"
            />
            <div v-else class="qr-placeholder">
              <svg viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="20" y="20" width="160" height="160" rx="8" fill="#F5F5F5" stroke="#E0E0E0" stroke-width="2"/>
                <rect x="40" y="40" width="40" height="40" rx="4" fill="#E8E8E8"/>
                <rect x="100" y="40" width="40" height="40" rx="4" fill="#E8E8E8"/>
                <rect x="160" y="40" width="0" height="0" fill="none"/>
                <rect x="40" y="100" width="40" height="40" rx="4" fill="#E8E8E8"/>
                <rect x="100" y="100" width="40" height="40" rx="4" fill="#E8E8E8"/>
                <rect x="160" y="100" width="0" height="0" fill="none"/>
                <rect x="160" y="160" width="0" height="0" fill="none"/>
                <rect x="40" y="160" width="40" height="0" fill="none"/>
                <text x="100" y="110" text-anchor="middle" fill="#BDBDBD" font-size="13">请上传</text>
                <text x="100" y="130" text-anchor="middle" fill="#BDBDBD" font-size="13">收款码图片</text>
              </svg>
            </div>
          </div>
          <p class="qr-tip">打开微信「扫一扫」即可捐赠</p>
        </div>
      </div>
    </div>

    <!-- 联系方式 -->
    <div class="section-title">联系方式</div>
    <p class="section-subtitle">如有合作意向或问题反馈，欢迎通过以下方式联系</p>

    <div class="contact-grid">
      <div class="contact-card">
        <div class="contact-icon-wrap">
          <el-icon :size="24"><Message /></el-icon>
        </div>
        <h4>邮箱</h4>
        <p>admin@3ks2u.com</p>
      </div>
      <div class="contact-card">
        <div class="contact-icon-wrap">
          <el-icon :size="24"><Link /></el-icon>
        </div>
        <h4>网站</h4>
        <p>www.3ks2u.com</p>
      </div>
      <div class="contact-card">
        <div class="contact-icon-wrap">
          <el-icon :size="24"><Clock /></el-icon>
        </div>
        <h4>响应时间</h4>
        <p>24 小时内回复</p>
      </div>
    </div>

    <!-- 鸣谢 -->
    <div class="thanks-section">
      <p>感谢每一位支持者 💙</p>
      <span>你的捐赠将用于服务器维护与内容创作</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Message, Link, Clock } from '@element-plus/icons-vue'

// 替换为你的实际收款码图片路径
const alipayQr = ref('/alipay-qr.webp')
const wechatQr = ref('/wechat-qr.webp')
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.contact-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 $gap-lg $gap-xl;
}

// ===== Hero =====
.contact-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $gap-xl 0 $gap-2xl;
  margin-bottom: $gap-xl;
  gap: $gap-xl;

  .hero-content {
    flex: 1;
    min-width: 0;

    h1 {
      font-size: 36px;
      font-weight: 700;
      color: $text-main;
      margin-bottom: $gap-sm;
      position: relative;
      padding-left: $gap-sm;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 4px;
        bottom: 4px;
        width: 4px;
        background: $border-dark;
        border-radius: 2px;
      }
    }

    p {
      font-size: 16px;
      color: $text-sub;
      line-height: 1.7;
    }
  }

  .hero-illustration {
    flex-shrink: 0;

    svg {
      width: 320px;
      height: 150px;
    }
  }
}

// ===== Section title (overrides global for spacing) =====
.section-title {
  font-size: 20px;
  font-weight: 600;
  color: $text-main;
  margin-bottom: $gap-sm;
  padding-bottom: $gap-sm;
  padding-left: $gap-sm;
  border-bottom: 1px solid $border;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 2px;
    bottom: 10px;
    width: 4px;
    background: $border-dark;
    border-radius: 2px;
  }
}

.section-subtitle {
  font-size: 14px;
  color: $text-muted;
  margin-bottom: $gap-lg;
  padding-left: $gap-sm;
}

// ===== 捐赠卡片 =====
.donate-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $gap-lg;
  margin-bottom: $gap-xl;
}

.donate-card {
  background: $white;
  border: 1px solid $border;
  border-radius: $radius-lg;
  padding: $gap-lg;
  transition: all $ease;

  &:hover {
    border-color: $border-dark;
    background: $bg-hover;
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: $gap-sm;
    margin-bottom: $gap-lg;
    padding-bottom: $gap-sm;
    border-bottom: 1px solid $border;

    h3 {
      font-size: 18px;
      font-weight: 600;
      color: $text-main;
    }

    span {
      font-size: 13px;
      color: $text-muted;
    }
  }

  .pay-icon {
    width: 40px;
    height: 40px;
    flex-shrink: 0;
  }
}

.qr-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-box {
  width: 200px;
  height: 200px;
  border-radius: $radius;
  overflow: hidden;
  background: $white;
  border: 1px solid $border;
  margin-bottom: $gap-sm;

  .qr-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }

  .qr-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    svg {
      width: 100%;
      height: 100%;
    }
  }
}

.qr-tip {
  font-size: 13px;
  color: $text-muted;
}

// ===== 联系方式卡片 =====
.contact-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $gap-md;
  margin-bottom: $gap-xl;
}

.contact-card {
  background: $white;
  border: 1px solid $border;
  border-radius: $radius-lg;
  padding: $gap-lg;
  text-align: center;
  transition: all $ease;
  cursor: default;

  &:hover {
    border-color: $border-dark;
    background: $bg-hover;
  }

  .contact-icon-wrap {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: $bg-section;
    color: $text-sub;
    margin-bottom: $gap-sm;
  }

  h4 {
    font-size: 16px;
    font-weight: 600;
    color: $text-main;
    margin-bottom: 4px;
  }

  p {
    font-size: 14px;
    color: $text-muted;
  }
}

// ===== 鸣谢 =====
.thanks-section {
  text-align: center;
  padding: $gap-lg;
  background: $bg-section;
  border-radius: $radius-lg;

  p {
    font-size: 20px;
    font-weight: 600;
    color: $text-main;
    margin-bottom: $gap-xs;
  }

  span {
    font-size: 14px;
    color: $text-muted;
  }
}

// ===== 响应式 =====
@media (max-width: 960px) {
  .contact-hero {
    flex-direction: column;
    text-align: center;

    .hero-content {
      h1 { font-size: 28px; }
    }

    .hero-illustration svg {
      width: 260px;
      height: 120px;
    }
  }

  .donate-grid {
    grid-template-columns: 1fr;
  }

  .contact-grid {
    grid-template-columns: 1fr;
  }
}
</style>
