package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import javax.swing.*;
import java.util.List;

public final class EquipmentPanel extends TablePanel<HallFacility> implements Refreshable {
    private final ApplicationServices app; private final AuthenticatedUser user;
    private record Choices(List<LectureHall> halls,List<Facility> facilities) { }
    public EquipmentPanel(ApplicationServices app, AuthenticatedUser user) {
        super(new String[]{"Hall ID","Hall","Facility ID","Facility","Total","Working","Not working"},
                f -> new Object[]{f.hallId(),f.hallLabel(),f.facilityId(),f.facilityName(),f.quantity(),f.workingQuantity(),f.quantity()-f.workingQuantity()});
        this.app=app; this.user=user; button("Refresh",this::refresh); button("Set hall equipment",this::setEquipment);
    }
    @Override public void refresh() { UiTasks.run(this,"Loading hall equipment...",app.catalog::equipment,this::setRows); }
    private void setEquipment() {
        HallFacility old=table.getSelectedRow()<0 ? null : selected();
        UiTasks.run(this,"Loading hall and facility choices...",() -> new Choices(app.catalog.halls(),app.catalog.facilities()),choices -> {
            FormPanel f=new FormPanel(); JComboBox<LookupOption> hall=f.choices("Hall",choices.halls().stream().map(LectureHall::option).toList(),false);
            JComboBox<LookupOption> facility=f.choices("Facility type",choices.facilities().stream().map(Facility::option).toList(),false);
            if (old != null) { FormPanel.select(hall,old.hallId()); FormPanel.select(facility,old.facilityId()); }
            JTextField quantity=f.text("Total quantity",old==null ? "1" : String.valueOf(old.quantity()));
            JTextField working=f.text("Working quantity",old==null ? "1" : String.valueOf(old.workingQuantity()));
            if (!f.confirm(this,"Set equipment quantities")) { return; }
            int h=FormPanel.requiredId(hall),id=FormPanel.requiredId(facility),q=FormPanel.integer(quantity,"Total quantity"),w=FormPanel.integer(working,"Working quantity");
            UiTasks.run(this,"Saving equipment quantities...",() -> { app.catalog.setEquipment(user,h,id,q,w); return true; },ignored -> refresh());
        });
    }
}
