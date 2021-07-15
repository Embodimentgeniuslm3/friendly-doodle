const puppeteer = require('puppeteer');
const assert = require('assert');
const cas = require('../../cas.js');

(async () => {
    const browser = await puppeteer.launch(cas.browserOptions());
    const page = await cas.newPage(browser);
    await page.goto("https://localhost:8443/cas/login");

    await cas.loginWith(page, "invalidlocation", "invalidlocation");

    // await page.waitForTimeout(5000)

    const header = await cas.innerText(page, "#content h2");
    assert(header === "You cannot login from this workstation.")
    
    await browser.close();
})();
