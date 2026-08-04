const fs = require('node:fs')
const path = require('node:path')

const configuredPlaywrightModule = process.env.PLAYWRIGHT_MODULE
const playwrightModule = configuredPlaywrightModule && (configuredPlaywrightModule.startsWith('.') || path.isAbsolute(configuredPlaywrightModule))
  ? path.resolve(process.cwd(), configuredPlaywrightModule)
  : configuredPlaywrightModule || 'playwright'
const { chromium } = require(playwrightModule)

const root = path.resolve(__dirname, '..')
const outputDir = path.join(root, 'output', 'playwright')
fs.mkdirSync(outputDir, { recursive: true })

const baseUrl = process.env.THREADMIND_FRONTEND_URL || 'http://127.0.0.1:5173'
const routes = ['/', '/topics/101', '/topics/new', '/assistant', '/admin', '/login']
const viewports = [
  { name: 'desktop', width: 1440, height: 1000 },
  { name: 'mobile', width: 390, height: 844 }
]

async function main() {
  const launchOptions = { headless: true }
  if (process.env.PLAYWRIGHT_CHANNEL) {
    launchOptions.channel = process.env.PLAYWRIGHT_CHANNEL
  }
  if (process.env.PLAYWRIGHT_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_EXECUTABLE_PATH
    delete launchOptions.channel
  }
  const browser = await chromium.launch(launchOptions)
  const results = []

  for (const viewport of viewports) {
    const page = await browser.newPage({ viewport })
    const consoleErrors = []
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text())
      }
    })

    for (const route of routes) {
      const response = await page.goto(baseUrl + route, { waitUntil: 'networkidle' })
      const title = await page.title()
      const bodyText = (await page.locator('body').innerText()).trim()
      const dimensions = await page.evaluate(() => ({
        width: document.documentElement.scrollWidth,
        height: document.documentElement.scrollHeight,
        clientWidth: document.documentElement.clientWidth
      }))
      const fileName = `${viewport.name}-${route === '/' ? 'home' : route.slice(1).replaceAll('/', '-')}.png`
      const screenshotPath = path.join(outputDir, fileName)
      await page.screenshot({ path: screenshotPath, fullPage: true })

      results.push({
        route,
        viewport: viewport.name,
        status: response ? response.status() : 0,
        title,
        textLength: bodyText.length,
        horizontalOverflow: dimensions.width > dimensions.clientWidth + 1,
        screenshot: path.relative(root, screenshotPath)
      })
    }

    await page.close()

    if (consoleErrors.length) {
      results.push({
        route: '*',
        viewport: viewport.name,
        consoleErrors
      })
    }
  }

  await browser.close()
  console.log(JSON.stringify(results, null, 2))
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
