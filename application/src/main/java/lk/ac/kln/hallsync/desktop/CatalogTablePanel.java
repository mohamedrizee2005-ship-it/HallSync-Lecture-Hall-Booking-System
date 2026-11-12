package lk.ac.kln.hallsync.desktop;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

public final class CatalogTablePanel<T> extends TablePanel<T> implements Refreshable {
    private final Callable<List<T>> loader;
    public CatalogTablePanel(String[] columns, Function<T,Object[]> cells, Callable<List<T>> loader) {
        super(columns,cells); this.loader=loader; button("Refresh",this::refresh);
    }
    @Override public void refresh() { UiTasks.run(this,"Loading reference data...",loader,this::setRows); }
    public void command(String title, Runnable action) { button(title,action); }
}
