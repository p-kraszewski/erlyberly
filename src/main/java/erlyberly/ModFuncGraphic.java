/**
 * erlyberly, erlang trace debugger
 * Copyright (C) 2016 Andy Till
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package erlyberly;

import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import ui.CellController;
import ui.FAIcon;

class ModFuncGraphic extends HBox implements CellController<ModFunc> {

    public interface TraceFn {
        void trace(ModFunc modFunc);
    }

    public interface IsTracedFn {
        boolean isTraced(ModFunc mf);
    }

    private static final String ICON_STYLE = "-fx-font-family: FontAwesome; -fx-font-size: 1em;";

    private final SimpleStringProperty text = new SimpleStringProperty();

    private final SimpleStringProperty exportIconText = new SimpleStringProperty();

    private final SimpleStringProperty exportToolTipText = new SimpleStringProperty();

    private final SimpleStringProperty tracedIconText = new SimpleStringProperty(AwesomeIcon.STAR_ALT.toString());

    private final SimpleBooleanProperty tracable = new SimpleBooleanProperty();

    private final TraceFn traceFn;

    private final IsTracedFn isTracedFn;

    private boolean showModuleName;

    private ModFunc modFunc;

    public ModFuncGraphic(TraceFn aTraceFn, IsTracedFn isTracedFn) {
        traceFn = aTraceFn;
        this.isTracedFn = isTracedFn;

        getStyleClass().add("mod-func-graphic");

        getChildren().addAll(
            exportIconGraphic(),
            traceIcon(),
            functionLabel()
        );
    }

    private FAIcon exportIconGraphic() {
        Tooltip tooltip;

        tooltip = new Tooltip();
        tooltip.textProperty().bind(exportToolTipText);

        FAIcon treeIcon;

        treeIcon = treeIcon(AwesomeIcon.SQUARE);
        treeIcon.textProperty().bind(exportIconText);
        treeIcon.setTooltip(tooltip);
        return treeIcon;
    }

    private FAIcon traceIcon() {
        FAIcon traceIcon;

        traceIcon = FAIcon.create().style(ICON_STYLE);
        traceIcon.textProperty().bind(tracedIconText);
        traceIcon.visibleProperty().bind(tracable);
        traceIcon.setTooltip(new Tooltip("Toggle tracing, double click on this star or ctrl+t when selected"));
        traceIcon.getStyleClass().add("erlyberly-icon-button");
        traceIcon.setOnMouseClicked((e) -> {
            if(e.getClickCount() == 2)
                traceFn.trace(modFunc);
        });
        return traceIcon;
    }

    private Label functionLabel() {
        Label label;

        label = new Label();
        label.textProperty().bind(text);

        return label;
    }

    private FAIcon treeIcon(AwesomeIcon treeIcon) {
        return FAIcon.create().icon(treeIcon).style(ICON_STYLE);
    }

    @Override
    public void updateItem(ModFunc item, boolean empty) {
        if (item == null || empty) {
            text.set(null);
        }
        else {
            if(isShowModuleName())
                text.set(item.toFullString());
            else
                text.set(item.toString());

            updateExportIcon(item);

            // no tracing of the whole module for now!
            tracable.set(!item.isModule());
        }
        modFunc = item;

        onTracesChange();
    }

    private void updateExportIcon(ModFunc item) {
        AwesomeIcon icon;
        String tooltipText;

        if(item.isModule()) {
            tooltipText = "Module";
            icon = AwesomeIcon.CUBE;
        }
        else if(item.isExported()) {
            tooltipText = "Exported function";
            icon = AwesomeIcon.UNLOCK_ALT;
        }
        else {
            tooltipText = "Unexported function";
            icon = AwesomeIcon.LOCK;
        }

        exportToolTipText.set(tooltipText);
        exportIconText.set(icon.toString());
    }

    public void onTracesChange() {
        if(modFunc != null && isTracedFn.isTraced(modFunc))
            tracedIconText.set(AwesomeIcon.CHECK_SQUARE_ALT.toString());
        else
            tracedIconText.set(AwesomeIcon.SQUARE_ALT.toString());
    }

    public boolean isShowModuleName() {
        return showModuleName;
    }

    public void setShowModuleName(boolean showModuleName) {
        this.showModuleName = showModuleName;
    }
}
