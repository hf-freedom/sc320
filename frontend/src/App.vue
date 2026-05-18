<template>
  <div id="app">
    <el-container class="main-container">
      <el-header class="header">
        <div class="logo">🌾 智慧农田灌溉系统</div>
        <div class="header-info">
          <span>{{ currentTime }}</span>
        </div>
      </el-header>
      <el-container>
        <el-aside width="220px" class="aside">
          <el-menu
            :default-active="$route.path"
            class="el-menu-vertical-demo"
            router
            background-color="#1F2D3D"
            text-color="#C0C4CC"
            active-text-color="#409EFF">
            <el-menu-item index="/">
              <i class="el-icon-s-home"></i>
              <span slot="title">系统概览</span>
            </el-menu-item>
            <el-menu-item index="/fields">
              <i class="el-icon-location-outline"></i>
              <span slot="title">地块管理</span>
            </el-menu-item>
            <el-menu-item index="/devices">
              <i class="el-icon-cpu"></i>
              <span slot="title">设备管理</span>
            </el-menu-item>
            <el-menu-item index="/plans">
              <i class="el-icon-date"></i>
              <span slot="title">灌溉计划</span>
            </el-menu-item>
            <el-menu-item index="/water">
              <i class="el-icon-umbrella"></i>
              <span slot="title">水源监控</span>
            </el-menu-item>
            <el-menu-item index="/report">
              <i class="el-icon-document"></i>
              <span slot="title">每日报告</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main class="main-content">
          <router-view/>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      currentTime: ''
    }
  },
  mounted() {
    this.updateTime()
    setInterval(this.updateTime, 1000)
  },
  methods: {
    updateTime() {
      const now = new Date()
      this.currentTime = now.toLocaleString('zh-CN')
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
}

.main-container {
  height: 100vh;
}

.header {
  background: linear-gradient(90deg, #1E6F5C, #2D8B6A);
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  font-size: 18px;
  font-weight: bold;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.logo {
  font-size: 22px;
}

.header-info {
  font-size: 14px;
  font-weight: normal;
}

.aside {
  background-color: #1F2D3D;
}

.main-content {
  background-color: #F5F7FA;
  padding: 20px;
}

.el-menu-vertical-demo:not(.el-menu--collapse) {
  width: 220px;
  min-height: 400px;
}
</style>
