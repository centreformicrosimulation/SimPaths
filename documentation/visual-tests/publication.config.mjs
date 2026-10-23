import { defineConfig } from '@playwright/test';
export default defineConfig({workers:1,testDir:'.',testMatch:'publication.spec.mjs',use:{baseURL:process.env.SIMPATHS_DOCS_URL || 'http://127.0.0.1:8316'},projects:[{name:'desktop',use:{viewport:{width:1280,height:720}}},{name:'mobile',use:{viewport:{width:390,height:844}}}]});
