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

package org.deidentifier.arx.gui.view.impl.menu;

import org.deidentifier.arx.gui.model.Model;
import org.deidentifier.arx.gui.resources.Resources;
import org.deidentifier.arx.gui.view.SWTUtil;
import org.deidentifier.arx.gui.view.def.IDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogQuasiIdentifiers extends TitleAreaDialog implements IDialog {

	  private Text txtAlphaDistinct;
	  private Text txtAlphaSeparation;

	  private double alphaDistinct;
	  private double alphaSeparation;
	  
	  private Model model;
	
    /**
     * Constructor.
     *
     * @param parentShell
     * @param controller
     */
    public DialogQuasiIdentifiers(final Shell parentShell, final Model model) {
        super(parentShell);
        this.model = model;
    }
    
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {

        // Create OK Button
        parent.setLayoutData(SWTUtil.createFillGridData());
        final Button okButton = createButton(parent,
                                             Window.OK,
                                             Resources.getMessage("AboutDialog.15"), true); //$NON-NLS-1$
        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                setReturnCode(Window.OK);
                close();
            }
        });
        
     // Create ShowAll Button
        final Button cancelButton = createButton(parent,
                Window.CANCEL,
                Resources.getMessage("QIDialog.0"), false); //$NON-NLS-1$
        
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                setReturnCode(Window.CANCEL);
                close();
            }
        });
    }
    
    @Override
    protected Control createContents(Composite parent) {
    	Control contents = super.createContents(parent);
        setTitle(Resources.getMessage("QIDialog.1")); //$NON-NLS-1$
        setMessage(Resources.getMessage("QIDialog.2"), IMessageProvider.INFORMATION); //$NON-NLS-1$
        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      Composite area = (Composite) super.createDialogArea(parent);
      Composite container = new Composite(area, SWT.NONE);
      container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      GridLayout layout = new GridLayout(2, false);
      container.setLayout(layout);

      createAlphaDistinct(container);
      createAlphaSeparation(container);

      return area;
    }

    private void createAlphaDistinct(Composite container) {
      Label lbtFirstName = new Label(container, SWT.NONE);
      lbtFirstName.setText("alpha distinct");

      GridData dataFirstName = new GridData();
      dataFirstName.grabExcessHorizontalSpace = true;
      dataFirstName.horizontalAlignment = GridData.FILL;

     txtAlphaDistinct = new Text(container, SWT.BORDER);
     txtAlphaDistinct.setLayoutData(dataFirstName);
     txtAlphaDistinct.insert(new Double(model.getDistinctThreshold()).toString());
    }
    
    private void createAlphaSeparation(Composite container) {
      Label lbtLastName = new Label(container, SWT.NONE);
      lbtLastName.setText("alpha separation");
      
      GridData dataLastName = new GridData();
      dataLastName.grabExcessHorizontalSpace = true;
      dataLastName.horizontalAlignment = GridData.FILL;
      
      txtAlphaSeparation = new Text(container, SWT.BORDER);
      txtAlphaSeparation.setLayoutData(dataLastName);
      txtAlphaSeparation.insert(new Double(model.getSeparationThreshold()).toString());
    }

    protected Control createDialogAreax(final Composite parent) {
        parent.setLayout(new GridLayout());
        return parent;
    }

    private void setThresholds() {
    	alphaDistinct = new Double(txtAlphaDistinct.getText());
    	alphaSeparation = new Double(txtAlphaSeparation.getText());
    	model.setQIThresholds(alphaDistinct, alphaSeparation, false);
    }

    @Override
    protected void okPressed() {
    	setThresholds();
    	super.okPressed();
    }
    
    @Override
    protected void cancelPressed() {
    	// Show All
        model.setQIThresholds(alphaDistinct, alphaSeparation, true);
        super.cancelPressed();
    }
    
    @Override
    protected boolean isResizable() {
        return false;
    }
}
