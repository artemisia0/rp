import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { use } from 'echarts/core';
import { BarChart, HeatmapChart, LineChart, PieChart } from 'echarts/charts';
import {
  DataZoomComponent,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  TitleComponent,
  VisualMapComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import VChart from 'vue-echarts';

import App from './App.vue';
import router from './router';

use([
  BarChart,
  HeatmapChart,
  LineChart,
  PieChart,
  DataZoomComponent,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  TitleComponent,
  VisualMapComponent,
  CanvasRenderer,
]);

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.component('VChart', VChart);

app.mount('#app');
