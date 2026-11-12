package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import javax.swing.*;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;

/** Small administrator forms for the six reference catalogues. */
public final class CataloguesPanel extends JPanel implements Refreshable {
    private final ApplicationServices app; private final AuthenticatedUser user; private final JTabbedPane tabs=new JTabbedPane();
    public CataloguesPanel(ApplicationServices app, AuthenticatedUser user) {
        super(new BorderLayout()); this.app=app; this.user=user;
        departments(); buildings(); facilities(); courses(); groups(); halls();
        tabs.addChangeListener(e -> refresh()); add(tabs);
    }
    @Override public void refresh() { if (tabs.getSelectedComponent() instanceof Refreshable panel) { panel.refresh(); } }
    private void departments() {
        CatalogTablePanel<Department> p=new CatalogTablePanel<>(new String[]{"ID","Department name"},d -> new Object[]{d.getDepartmentId(),d.getDepartmentName()},app.departments::list);
        p.command("Add department",() -> { FormPanel f=new FormPanel(); JTextField name=f.text("Department name",""); if (!f.confirm(this,"Add department")) { return; }
            String value=name.getText(); UiTasks.run(this,"Adding department...",() -> app.departments.add(user,value),ignored -> p.refresh()); });
        tabs.addTab("Departments",p);
    }
    private void buildings() {
        CatalogTablePanel<Building> p=new CatalogTablePanel<>(new String[]{"ID","Code","Building name","Location"},b -> new Object[]{b.id(),b.code(),b.name(),b.location()},app.catalog::buildings);
        p.command("Add building",() -> { FormPanel f=new FormPanel(); JTextField code=f.text("Building code",""),name=f.text("Name",""),location=f.text("Location (optional)","");
            if (!f.confirm(this,"Add building")) { return; } String c=code.getText(),n=name.getText(),l=location.getText();
            UiTasks.run(this,"Adding building...",() -> app.catalog.addBuilding(user,c,n,l),ignored -> p.refresh()); });
        tabs.addTab("Buildings",p);
    }
    private void facilities() {
        CatalogTablePanel<Facility> p=new CatalogTablePanel<>(new String[]{"ID","Facility name","Description"},f -> new Object[]{f.id(),f.name(),f.description()},app.catalog::facilities);
        p.command("Add facility type",() -> { FormPanel f=new FormPanel(); JTextField name=f.text("Name",""),description=f.text("Description (optional)","");
            if (!f.confirm(this,"Add facility type")) { return; } String n=name.getText(),d=description.getText();
            UiTasks.run(this,"Adding facility...",() -> app.catalog.addFacility(user,n,d),ignored -> p.refresh()); });
        tabs.addTab("Facility types",p);
    }
    private void courses() {
        CatalogTablePanel<CourseUnit> p=new CatalogTablePanel<>(new String[]{"ID","Department ID","Course code","Course title"},c -> new Object[]{c.id(),c.departmentId(),c.code(),c.title()},app.catalog::courses);
        p.command("Add course",() -> departmentChoices(choices -> { FormPanel f=new FormPanel(); JComboBox<LookupOption> department=f.choices("Department",choices,false);
            JTextField code=f.text("Course code",""),title=f.text("Course title",""); if (!f.confirm(this,"Add course")) { return; }
            int d=FormPanel.requiredId(department); String c=code.getText(),t=title.getText();
            UiTasks.run(this,"Adding course...",() -> app.catalog.addCourse(user,d,c,t),ignored -> p.refresh()); }));
        tabs.addTab("Courses",p);
    }
    private void groups() {
        CatalogTablePanel<StudentGroup> p=new CatalogTablePanel<>(new String[]{"ID","Department ID","Group name","Intake year","Study year"},g -> new Object[]{g.id(),g.departmentId(),g.name(),g.intakeYear(),g.studyYear()},app.catalog::groups);
        p.command("Add student group",() -> departmentChoices(choices -> { FormPanel f=new FormPanel(); JComboBox<LookupOption> department=f.choices("Department",choices,false);
            JTextField name=f.text("Group name",""),intake=f.text("Intake year","2026"),year=f.text("Study year","1"); if (!f.confirm(this,"Add student group")) { return; }
            int d=FormPanel.requiredId(department),i=FormPanel.integer(intake,"Intake year"),y=FormPanel.integer(year,"Study year"); String n=name.getText();
            UiTasks.run(this,"Adding student group...",() -> app.catalog.addGroup(user,d,n,i,y),ignored -> p.refresh()); }));
        tabs.addTab("Student groups",p);
    }
    private void halls() {
        CatalogTablePanel<LectureHall> p=new CatalogTablePanel<>(new String[]{"ID","Hall","Building name","Floor","Capacity","Status"},h -> new Object[]{h.id(),h.label(),h.buildingName(),h.floor(),h.capacity(),h.status()},app.catalog::halls);
        p.command("Add hall",() -> UiTasks.run(this,"Loading buildings...",app.catalog::buildings,buildings -> {
            FormPanel f=new FormPanel(); JComboBox<LookupOption> building=f.choices("Building",buildings.stream().map(Building::option).toList(),false);
            JTextField number=f.text("Hall number",""),floor=f.text("Floor (optional), e.g. Ground",""),capacity=f.text("Capacity","50");
            if (!f.confirm(this,"Add lecture hall")) { return; } int b=FormPanel.requiredId(building),c=FormPanel.integer(capacity,"Capacity"); String n=number.getText(),level=floor.getText();
            UiTasks.run(this,"Adding lecture hall...",() -> app.catalog.addHall(user,b,n,level,c),ignored -> p.refresh());
        }));
        p.command("Change capacity / status",() -> {
            LectureHall hall=p.selected(); FormPanel f=new FormPanel(); JTextField capacity=f.text("Capacity",String.valueOf(hall.capacity()));
            JComboBox<String> status=f.field("Operational status",new JComboBox<>(new String[]{"Active","Inactive"})); status.setSelectedItem(hall.status());
            if (!f.confirm(this,hall.label())) { return; } int c=FormPanel.integer(capacity,"Capacity"); String s=(String)status.getSelectedItem();
            UiTasks.run(this,"Checking existing reservations...",() -> { app.catalog.updateHall(user,hall.id(),c,s); return true; },ignored -> p.refresh());
        }); tabs.addTab("Lecture halls",p);
    }
    private void departmentChoices(Consumer<List<LookupOption>> next) {
        UiTasks.run(this,"Loading departments...",() -> app.departments.list().stream().map(d -> new LookupOption(d.getDepartmentId(),d.getDepartmentName())).toList(),next);
    }
}
