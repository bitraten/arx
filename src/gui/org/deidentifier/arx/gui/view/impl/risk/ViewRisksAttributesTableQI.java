/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deidentifier.arx.gui.view.impl.risk;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.gui.Controller;
import org.deidentifier.arx.gui.model.ModelEvent;
import org.deidentifier.arx.gui.model.ModelEvent.ModelPart;
import org.deidentifier.arx.gui.model.ModelRisk.ViewRiskType;
import org.deidentifier.arx.gui.resources.Resources;
import org.deidentifier.arx.gui.view.SWTUtil;
import org.deidentifier.arx.gui.view.impl.common.ClipboardHandlerTable;
import org.deidentifier.arx.gui.view.impl.common.ComponentStatusLabelProgressProvider;
import org.deidentifier.arx.gui.view.impl.common.async.Analysis;
import org.deidentifier.arx.gui.view.impl.common.async.AnalysisContext;
import org.deidentifier.arx.gui.view.impl.common.async.AnalysisManager;
import org.deidentifier.arx.risk.RiskEstimateBuilderInterruptible;
import org.deidentifier.arx.risk.RiskModelAttributesQI;
import org.deidentifier.arx.risk.RiskModelAttributesQI.QuasiIdentifierRisk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import de.linearbits.swt.table.DynamicTable;
import de.linearbits.swt.table.DynamicTableColumn;

public class ViewRisksAttributesTableQI extends ViewRisks<AnalysisContextRisk> {

    /** View */
    private DecimalFormat     format = new DecimalFormat("##0.000"); //$NON-NLS-1$

    /** View */
    private Composite         root;

    /** View */
    private DynamicTable      table;

    /** Internal stuff. */
    private AnalysisManager   manager;
    
    private Controller		  controller;

    /**
     * Creates a new instance.
     *
     * @param parent
     * @param controller
     * @param target
     * @param reset
     */
    public ViewRisksAttributesTableQI(final Composite parent,
                                    final Controller controller,
                                    final ModelPart target,
                                    final ModelPart reset) {
        
        super(parent, controller, target, reset);
        controller.addListener(ModelPart.SELECTED_QUASI_IDENTIFIERS, this);
        controller.addListener(ModelPart.POPULATION_MODEL, this);
        this.manager = new AnalysisManager(parent.getDisplay());
        this.controller = controller;
    }
    
    @Override
    public void update(ModelEvent event) {
        super.update(event);
        if (event.part == ModelPart.SELECTED_QUASI_IDENTIFIERS || event.part == ModelPart.POPULATION_MODEL) {
            triggerUpdate();
        }
    }

    /**
     * Creates a table item
     * @param risks
     */
    private void createItem(QuasiIdentifierRisk risks) {
        final TableItem item = new TableItem(table, SWT.NONE);
        List<String> list = new ArrayList<String>();
        list.addAll(risks.getIdentifier());
        Collections.sort(list);
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<list.size(); i++) {
            builder.append(list.get(i));
            if (i < list.size() - 1){
                builder.append(", "); //$NON-NLS-1$
            }
        }
        item.setText(0, builder.toString());
        item.setText(1, format.format(risks.getAlphaDistinct() * 100d));
        item.setText(2, format.format(risks.getAlphaSeparation() * 100d));
    }

    @Override
    protected Control createControl(Composite parent) {

        this.root = new Composite(parent, SWT.NONE);
        this.root.setLayout(new FillLayout());

        table = SWTUtil.createTableDynamic(root, SWT.SINGLE | SWT.BORDER |
                                       SWT.V_SCROLL | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setMenu(new ClipboardHandlerTable(table).getMenu());

        DynamicTableColumn c = new DynamicTableColumn(table, SWT.LEFT);
        c.setWidth("70%"); //$NON-NLS-1$ //$NON-NLS-2$
        c.setText(Resources.getMessage("RiskAnalysis.19")); //$NON-NLS-1$
        c.setResizable(true);
        c = new DynamicTableColumn(table, SWT.LEFT);
        c.setWidth("15%"); //$NON-NLS-1$ //$NON-NLS-2$
        c.setText(Resources.getMessage("RiskAnalysis.26")); //$NON-NLS-1$
        c.setResizable(true);
        c = new DynamicTableColumn(table, SWT.LEFT);
        c.setWidth("15%"); //$NON-NLS-1$ //$NON-NLS-2$
        c.setText(Resources.getMessage("RiskAnalysis.27")); //$NON-NLS-1$
        c.setResizable(true);
        for (final TableColumn col : table.getColumns()) {
            col.pack();
        }
        SWTUtil.createGenericTooltip(table);
        return root;
    }

    @Override
    protected AnalysisContextRisk createViewConfig(AnalysisContext context) {
        return new AnalysisContextRisk(context);
    }

    @Override
    protected void doReset() {
        if (this.manager != null) {
            this.manager.stop();
        }
        table.setRedraw(false);
        for (final TableItem i : table.getItems()) {
            i.dispose();
        }
        table.setRedraw(true);
        setStatusEmpty();
    }

    @Override
    protected void doUpdate(final AnalysisContextRisk context) {
        
        // Enable/disable
        final RiskEstimateBuilderInterruptible builder = getBuilder(context, context.context.getModel().getSelectedQuasiIdentifiers());
        if (!this.isEnabled() || builder == null) {
            if (manager != null) {
                manager.stop();
            }
            this.setStatusEmpty();
            return;
        }
        
        // Create an analysis
        Analysis analysis = new Analysis() {

            // The statistics builder
            private boolean                  stopped = false;
            private RiskModelAttributesQI    risks;
            
            @Override
            public int getProgress() {
                return builder.getProgress();
            }

            @Override
            public void onError() {
                setStatusEmpty();
            }

            @Override
            public void onFinish() {

                if (stopped || !isEnabled()) {
                    return;
                }

                // Update chart
                for (final TableItem i : table.getItems()) {
                    i.dispose();
                }

                Set<String> foundQIs = new HashSet<String>();
                
                // For all sizes
                for (QuasiIdentifierRisk item : risks.getAttributeRisks()) {
                	
                	// Add found quasi-identifiers to set, if they were filtered by a threshold.
                	if(!context.context.getModel().getShowAll()) {
                    	foundQIs.addAll(item.getIdentifier());
                	}
                    createItem(item);
                }
                
				// Mark found quasi-identifiers as identifying.
				for (String qi : foundQIs) {
					context.context.getModel().getInputDefinition().setAttributeType(qi, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
					// Update the views
					controller.update(new ModelEvent(this,
									  ModelPart.ATTRIBUTE_TYPE,
									  qi));
				}

                for (final TableColumn col : table.getColumns()) {
                    col.pack();
                }

                if (risks.getAttributeRisks().length==0) {
                    setStatusEmpty();
                } else {
                    setStatusDone();
                }

                table.layout();
                table.redraw();
            }

            @Override
            public void onInterrupt() {
                if (!isEnabled() || !isValid()) {
                    setStatusEmpty();
                } else {
                    setStatusWorking();
                }
            }

            @Override
            public void run() throws InterruptedException {

                // Timestamp
                long time = System.currentTimeMillis();
                
                if(!context.context.getModel().getQiThresholdsSet()) {
                	// Standard Values
                	context.context.getModel().setQIThresholds(0.6, 0.8, false);
                }
                
                risks = builder.getQIRisks(context.context.getModel().getShowAll(),
					   	   context.context.getModel().getDistinctThreshold(),
					   	   context.context.getModel().getSeparationThreshold()
					   	  );

                // Our users are patient
                while (System.currentTimeMillis() - time < MINIMAL_WORKING_TIME && !stopped) {
                    Thread.sleep(10);
                }
            }

            @Override
            public void stop() {
                if (builder != null) builder.interrupt();
                this.stopped = true;
            }
        };

        this.manager.start(analysis);
    }

    @Override
    protected ComponentStatusLabelProgressProvider getProgressProvider() {
        return new ComponentStatusLabelProgressProvider(){
            public int getProgress() {
                if (manager == null) {
                    return 0;
                } else {
                    return manager.getProgress();
                }
            }
        };
    }
    
    @Override
    protected ViewRiskType getViewType() {
        return ViewRiskType.ATTRIBUTES;
    }

    /**
     * Is an analysis running
     */
    protected boolean isRunning() {
        return manager != null && manager.isRunning();
    }
}
