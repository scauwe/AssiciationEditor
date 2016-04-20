/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.dialog;

import info.vancauwenberge.idm.association.Activator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AssociationStatusDialog extends MessageDialog {
	private static final int extendedAreaLineHight = 5;
	private static Image image = null;
	private String[] details;
	private Text detailsComposit;
	private boolean detailsVisible = false;
	private int detailsHeight = 0;

	public AssociationStatusDialog(Shell parentShell, String summary, String[] details) {
		super(parentShell,"Association job result",getDialogImage(),summary,INFORMATION,new String[]{"OK","Details >>"},0);
		this.details = details;
	}

	
	public static Image getDialogImage(){
		if (image == null){
			ImageDescriptor imageDescr = Activator.getImageDescriptor("icons/sample2.png");
			if (imageDescr != null)
				image = (Image)imageDescr.createResource(null);
		}
		return image;
	}
	
    /**
     * @see Dialog#createDialogArea(Composite)
     */
    /*protected Control createDialogArea(Composite parent) {
        Composite dialogAreaComposite = (Composite) super.createDialogArea(parent);
        createExtendedDialogArea(dialogAreaComposite);
        return dialogAreaComposite;
    }*/
    
    
    protected void buttonPressed(int buttonId) {
    	if (buttonId==1){
	    	handleDetailsButtonSelect();
    	}else
    		super.buttonPressed(buttonId);
    }


    protected void handleDetailsButtonSelect() {
        Shell shell = getShell();
        Point shellSize = shell.getSize();
    	detailsVisible = !detailsVisible;
		detailsComposit.setVisible(detailsVisible);
		GridData detailsGridData = ((GridData)detailsComposit.getLayoutData());
		detailsGridData.exclude=!detailsVisible;
		detailsComposit.getParent().layout(false);
		Button detailsButton = getButton(1);
		if (detailsVisible){
			detailsButton.setText("Details <<");
			detailsHeight = detailsComposit.computeTrim(0, 0, 0, convertHeightInCharsToPixels(extendedAreaLineHight)).height;
			detailsGridData.minimumHeight = detailsHeight;
			detailsGridData.heightHint = detailsHeight;
			shell.setSize(shellSize.x, shellSize.y + detailsHeight);
			}
		else{
			detailsButton.setText("Details >>");
			shell.setSize(shellSize.x, shellSize.y - detailsHeight);
			}
	}

	/**
	 * Create the extensions to the dialog area.
	 * @param parent
	 */
	protected void createDialogAndButtonArea(Composite parent) {
		super.createDialogAndButtonArea(parent);
		//Add the extendedDialog area
		createExtendedDialogArea(parent);
	}


	private void createExtendedDialogArea(Composite parent){
	/*
		detailsComposit = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		int height = JFaceResources.getDefaultFont().getFontData()[0].getHeight();
		detailsComposit.getVerticalBar().setIncrement(height * 2);
		detailsComposit.setExpandHorizontal(true);
		detailsComposit.setExpandVertical(true);
		detailsComposit.setMinHeight(convertHeightInCharsToPixels(extendedAreaLineHight));
		detailsComposit.setVisible(detailsVisible);
		detailsComposit.setAlwaysShowScrollBars(true);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 0).hint(SWT.DEFAULT,detailsHeight).exclude(!detailsVisible).applyTo(detailsComposit);

		Composite control = new Composite(detailsComposit, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		control.setLayout(layout);
		control.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		detailsComposit.setContent(control);
		StringBuilder builder = new StringBuilder();
        //Add the message details to the Composite
        for (int i = 0; i < details.length; i++) {
			String aDetail = details[i];
			builder.append(aDetail).append("\n");
		}
		if (builder.length()<=0)
		builder.append("No details provided");
		
		Text aLabel = new Text(control, SWT.MULTI);
		aLabel.setText(builder.toString());
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = SWT.LEFT;
		aLabel.setLayoutData(gridData);
		aLabel.setEditable(false);
		aLabel.setVisible(true);
*/

		StringBuilder builder = new StringBuilder();
        //Add the message details to the Composite
        for (int i = 0; i < details.length; i++) {
			String aDetail = details[i];
			builder.append(aDetail).append("\n");
		}
		if (builder.length()<=0)
			builder.append("No details provided");
		detailsComposit = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 0).hint(SWT.DEFAULT,convertHeightInCharsToPixels(extendedAreaLineHight)).exclude(!detailsVisible).applyTo(detailsComposit);
		detailsComposit.setVisible(detailsVisible);
		detailsComposit.setEditable(false);
		detailsComposit.setText(builder.toString());
		
	}
/*
	private void createExtendedDialogArea(Composite parent) {
		Activator.log("creating createExtendedDialogArea");
		viewerComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 1;
        layout.marginWidth = 1;
        viewerComposite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.horizontalSpan = 2;
        viewerComposite.setLayoutData(data);
		
		
        //Add the message details to the Composite
        for (int i = 0; i < details.length; i++) {
			String aDetail = details[i];
			Activator.log("Adding label:"+aDetail);
			Label aLabel = new Label(viewerComposite, SWT.NONE);
			aLabel.setText(aDetail);
			GridData gridData = new GridData();
			gridData.grabExcessHorizontalSpace = true;
			gridData.horizontalAlignment = GridData.BEGINNING;
			aLabel.setLayoutData(gridData);
			aLabel.setVisible(true);
		}
	}*/
	
	
}
