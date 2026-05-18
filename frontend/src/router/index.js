import Vue from 'vue'
import VueRouter from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import Fields from '../views/Fields.vue'
import Devices from '../views/Devices.vue'
import Plans from '../views/Plans.vue'
import Water from '../views/Water.vue'
import Report from '../views/Report.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard
  },
  {
    path: '/fields',
    name: 'Fields',
    component: Fields
  },
  {
    path: '/devices',
    name: 'Devices',
    component: Devices
  },
  {
    path: '/plans',
    name: 'Plans',
    component: Plans
  },
  {
    path: '/water',
    name: 'Water',
    component: Water
  },
  {
    path: '/report',
    name: 'Report',
    component: Report
  }
]

const router = new VueRouter({
  mode: 'history',
  routes
})

export default router
