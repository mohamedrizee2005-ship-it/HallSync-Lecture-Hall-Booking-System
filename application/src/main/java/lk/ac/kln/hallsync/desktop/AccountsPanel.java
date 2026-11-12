package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import javax.swing.*;
import java.util.List;

public final class AccountsPanel extends TablePanel<AccountSummary> implements Refreshable {
    private final ApplicationServices app; private final AuthenticatedUser user;
    private final JComboBox<LookupOption> department=new JComboBox<>();
    private record Loaded(List<LookupOption> departments,List<AccountSummary> accounts) { }
    public AccountsPanel(ApplicationServices app, AuthenticatedUser user) {
        super(new String[]{"Account ID","Department","Full name","Email","Username","Role","Active"},
                a -> new Object[]{a.id(),a.department(),a.fullName(),a.email(),a.username(),a.role(),a.active()});
        this.app=app; this.user=user; department.addItem(new LookupOption(0,"All departments")); filters.add(new JLabel("Department:")); filters.add(department);
        button("Load / refresh",this::refresh); button("Provision account",this::create);
    }
    @Override public void refresh() {
        Integer filter=FormPanel.optionalId(department);
        UiTasks.run(this,"Loading accounts and department choices...",() -> new Loaded(choices(),app.accounts.list(user,filter)),loaded -> {
            department.removeAllItems(); department.addItem(new LookupOption(0,"All departments")); loaded.departments().forEach(department::addItem);
            FormPanel.select(department,filter); setRows(loaded.accounts());
        });
    }
    private List<LookupOption> choices() throws java.sql.SQLException {
        return app.departments.list().stream().map(d -> new LookupOption(d.getDepartmentId(),d.getDepartmentName())).toList();
    }
    private void create() { UiTasks.run(this,"Loading departments...",this::choices,
            choices -> new AccountDialog(SwingUtilities.getWindowAncestor(this),app,user,choices,this::refresh).setVisible(true)); }
}
