"""Exercise the browser interface against the real Java/MySQL server in CI."""
from pathlib import Path
from datetime import date,timedelta
import json,os,re,subprocess
from playwright.sync_api import sync_playwright,expect

ROOT=Path(__file__).resolve().parents[2]
def run(config,env):
    process=subprocess.Popen(['java','-Dhallsync.noBrowser=true','-Dhallsync.port=0','-jar',str(ROOT/'application/target/hallsync.jar'),str(config)],
                             cwd=ROOT/'application',env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
    checks=[]
    def ok(label):checks.append(label);print('PASS - '+label)
    try:
        line=process.stdout.readline().strip()
        assert line.startswith('HallSync is ready: '), 'Browser server failed to start'
        url=line.split('HallSync is ready: ',1)[1]
        with sync_playwright() as p:
            browser=p.chromium.launch()
            context=browser.new_context(viewport={'width':1440,'height':1050},reduced_motion='reduce')
            page=context.new_page();errors=[];page.on('pageerror',lambda error:errors.append(str(error)))
            page.goto(url);expect(page.get_by_role('heading',name='Welcome back.')).to_be_visible();ok('Browser sign-in screen renders')
            page.screenshot(path=str(ROOT/'application/target/web-signin.png'),full_page=True)
            def sign_in(username,password):
                page.locator('#login-form input[name=username]').fill(username)
                page.locator('#login-form input[name=password]').fill(password)
                page.get_by_role('button',name='Sign in to HallSync').click()
                expect(page.locator('.sidebar')).to_be_visible()
            sign_in('ci_admin',env['HALLSYNC_APPLICATION_TEST_PASSWORD']);ok('Administrator signs in through the real web form')
            expect(page.locator('.room-card').first).to_be_visible();ok('Overview renders live room cards')
            page.screenshot(path=str(ROOT/'application/target/web-overview.png'),full_page=True)
            page.get_by_role('button',name='Find a hall',exact=True).click()
            expect(page.get_by_role('heading',name='Find your next space.')).to_be_visible()
            f=page.locator('#availability-form');f.locator('[name=date]').fill((date.today()+timedelta(days=50)).isoformat());f.locator('[name=start]').fill('14:00');f.locator('[name=end]').fill('15:00');f.locator('[name=attendance]').fill('20');f.get_by_role('button',name='Find a hall').click()
            expect(page.locator('#space-count')).to_contain_text('available for your time');ok('Availability search shows server-filtered results')
            page.screenshot(path=str(ROOT/'application/target/web-spaces.png'),full_page=True)
            page.locator('#room-query').fill('WEB');expect(page.locator('.room-card')).to_have_count(1);ok('Room search filters without losing the selected time')
            page.get_by_role('button',name='View space').click();expect(page.locator('#modal')).to_be_visible();expect(page.locator('#modal')).to_contain_text('Web Display');ok('Room detail shows live working equipment')
            page.get_by_role('button',name='Book this space').click();expect(page.locator('#wizard-form')).to_be_visible();ok('Room detail opens the guided booking flow')
            page.locator('#wizard-form [name=type]').select_option('Event');page.locator('#wizard-form [name=purpose]').fill('Browser UI smoke event');page.locator('#wizard-form').get_by_role('button',name='Continue').click()
            expect(page.locator('#modal')).to_contain_text('For events and exams');page.locator('#wizard-form').get_by_role('button',name='Continue').click()
            expect(page.locator('#modal')).to_contain_text('Responsible requester');ok('Booking review shows the signed-in requester')
            page.get_by_role('button',name='Send booking request').click();expect(page.get_by_role('heading',name=re.compile(r'Booking #\d+ submitted'))).to_be_visible();ok('Guided booking submits a Pending request to MySQL')
            page.get_by_role('button',name='View my bookings').click();expect(page.locator('.booking-card').filter(has_text='Browser UI smoke event')).to_be_visible();ok('New request appears in personal bookings')
            page.locator('[data-nav=reviews]').click();expect(page.locator('.booking-list')).to_contain_text('Another admin must review');ok('Own-request review restriction is visible')
            page.locator('[data-nav=catalog]').click();page.get_by_role('button',name=re.compile('Departments')).click();expect(page.locator('table')).to_contain_text('Industrial Management');ok('Campus manager loads database catalogues')
            page.locator('[data-nav=people]').click();expect(page.locator('table')).to_contain_text('web_lecturer');ok('People screen displays provisioned accounts')
            page.get_by_role('button',name='Add person').click();expect(page.locator('#person-form [name=password]')).to_have_attribute('minlength','8');page.get_by_role('button',name='Cancel',exact=True).click();ok('Account forms use an eight-character minimum')
            page.locator('[data-nav=care]').click();expect(page.locator('table')).to_contain_text('Web Display');page.get_by_role('button',name='Maintenance & closures').click();expect(page.locator('#main')).to_contain_text('Web maintenance');ok('Equipment and retained block history render')
            page.locator('[data-nav=reports]').click();expect(page.locator('.metric-large').first).to_be_visible();expect(page.locator('table')).to_be_visible();ok('Reports render actual database totals and rows')
            with page.expect_download() as event:page.get_by_role('link',name='Export CSV').click()
            download=event.value;assert download.suggested_filename.endswith('.csv');ok('Report CSV downloads from the authenticated server')
            page.screenshot(path=str(ROOT/'application/target/web-reports.png'),full_page=True)
            page.locator('[data-nav=schedule]').click();expect(page.locator('.week-strip')).to_be_visible();ok('Weekly schedule is available')
            page.set_viewport_size({'width':390,'height':844});expect(page.get_by_role('button',name='Open navigation')).to_be_visible();page.get_by_role('button',name='Open navigation').click();page.locator('[data-nav=overview]').click();expect(page.locator('.room-card').first).to_be_visible();assert page.evaluate('document.documentElement.scrollWidth<=innerWidth'), 'Mobile page overflows horizontally';ok('Mobile navigation and layout fit a narrow screen')
            page.screenshot(path=str(ROOT/'application/target/web-mobile.png'),full_page=True)
            page.get_by_role('button',name='Open navigation').click();page.get_by_role('button',name='Sign out',exact=True).click();expect(page.locator('#login-form')).to_be_visible();ok('Browser sign-out returns to sign-in')
            page.set_viewport_size({'width':1440,'height':1050});sign_in('web_student','eight123');expect(page.locator('[data-nav=reviews]')).to_have_count(0);expect(page.get_by_role('button',name='New booking')).to_have_count(0);ok('Student navigation excludes privileged booking/admin actions')
            assert not errors, errors
            ok('Browser completes without JavaScript errors')
            browser.close()
        return checks
    finally:
        process.terminate()
        try:process.communicate(timeout=10)
        except subprocess.TimeoutExpired:process.kill();process.communicate()
