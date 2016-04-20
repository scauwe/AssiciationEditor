/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.novell.application.console.snapin.ObjectEntry;
import com.novell.core.Core;
import com.novell.core.datatools.access.nds.DSUtil;
import com.novell.core.ui.SWTUtil;
import com.novell.core.ui.layout.LayoutUtil;
import com.novell.core.util.GridDialogBase;
import com.novell.core.util.NLS;
import com.novell.idm.deploy.internal.DeployMessages;

public class SelectDriverDialog extends GridDialogBase
  implements SelectionListener
{
  private boolean m_cancelled;
  private boolean m_oked;
  private ObjectEntry[] m_driverOEs;
  private HashMap<Button, ObjectEntry> m_buttonMap = new HashMap<Button, ObjectEntry>();
  private LinkedList<ObjectEntry> m_selectedList = new LinkedList<ObjectEntry>();
  private ArrayList<Button> m_buttonList = new ArrayList<Button>();
private SelectionType selectionType;
  private static final int SC_HEIGHT = 250;
  private static final int SC_WIDTH = 500;

  public enum SelectionType{
	  SINGLE(SWT.RADIO),
	  MULTI(SWT.CHECK);
	  private int buttonType;

	  private SelectionType(int buttonType){
		  this.buttonType = buttonType;
	  }

	public int getButtonType() {
		return buttonType;
	}
	  
  }
  public SelectDriverDialog(Shell shell, ObjectEntry[] objectEntries, SelectionType selectionType)
  {
    super(shell);
    this.m_driverOEs = objectEntries;
    this.selectionType = selectionType;
    open();
  }

  protected void finalize()
  {
    release();
  }

  public boolean getCancelled()
  {
    return this.m_cancelled;
  }

  public boolean getOked()
  {
    return this.m_oked;
  }

  public List<ObjectEntry> getSelectedList()
  {
    return this.m_selectedList;
  }
  public ObjectEntry getSelectedOE()
  {
    return this.m_selectedList.getFirst();
  }

  public void release()
  {
    m_cancelled = false;
    m_oked = false;
    m_selectedList = null;
    m_buttonList = null;
    m_buttonMap = null;
    m_driverOEs = null;
    selectionType = null;
  }

  public void widgetDefaultSelected(SelectionEvent paramSelectionEvent)
  {
  }

  public void widgetSelected(SelectionEvent paramSelectionEvent)
  {
  }

  protected void buttonPressed(int paramInt)
  {
    int i = 0;
    switch (paramInt)
    {
    default:
      Core.internalErrorDlg(NLS.bind(DeployMessages.SelectDriverDlg_BogusPB, String.valueOf(paramInt)));
      i = 1;
      break;
    case 1:
      i = 1;
      break;
    case 0:
      i = 1;
    }
    if (i != 0)
      super.buttonPressed(paramInt);
  }

  protected void cancelPressed()
  {
    super.cancelPressed();
    this.m_cancelled = true;
    this.m_oked = false;
  }

  protected void configureShell(Shell paramShell)
  {
    super.configureShell(paramShell);
    paramShell.setText(DeployMessages.SelectDriversDlg_Title);
  }

  protected Control createDialogArea(Composite paramComposite)
  {
    Composite localComposite = (Composite)super.createDialogArea(paramComposite);
    layoutDialog(localComposite);
    /*
    if (JavaUtil.hasString(this.m_importHelpContext))
      Core.wireupHelp(DeployPlugin.getInstance(), localComposite, this.m_importHelpContext);*/
    return localComposite;
  }

  protected void okPressed()
  {
    Iterator<Button> buttonIter = this.m_buttonList.iterator();
    while (buttonIter.hasNext())
    {
      Button aButton = (Button)buttonIter.next();
      if (!aButton.getSelection())
        continue;
      ObjectEntry localObjectEntry = (ObjectEntry)this.m_buttonMap.get(aButton);
      this.m_selectedList.add(localObjectEntry);
    }
    super.okPressed();
    this.m_cancelled = false;
    this.m_oked = true;
  }


  private void layoutDialog(Composite paramComposite)
  {
    initializeDialogUnits(paramComposite);
    paramComposite.setLayout(buildGridLayout());
    Composite localComposite1 = new Composite(paramComposite, 0);
    localComposite1.setLayout(buildGridLayout(1));
    localComposite1.setLayoutData(buildGridData(SC_WIDTH));
    addTextLabel(localComposite1, "Multiple driver objects are selected. Please select the driver you want to work on.");
    ScrolledComposite m_scrolledComposite = new ScrolledComposite(paramComposite, 2560);
    m_scrolledComposite.setExpandHorizontal(true);
    m_scrolledComposite.setExpandVertical(false);
    m_scrolledComposite.getVerticalBar().setIncrement(charHeight());
    GridData localGridData = new GridData(768);
    localGridData.heightHint = SC_HEIGHT;
    m_scrolledComposite.setLayoutData(localGridData);
    Composite localComposite2 = new Composite(m_scrolledComposite, 0);
    localComposite2.setLayout(buildGridLayout());
    m_scrolledComposite.setContent(localComposite2);
    new UpdateComposite(localComposite2, 0);
    Point localPoint = SWTUtil.getPreferredSize(localComposite2);
    m_scrolledComposite.setMinHeight(localPoint.y);
    m_scrolledComposite.setMinWidth(localPoint.x);
    localComposite2.setSize(localPoint);
  }

  private class UpdateComposite extends Composite
  {
    UpdateComposite(Composite parent, int style)
    {
      super(parent, style);
      addButtons(parent);
    }

    private void addButtons(Composite paramComposite)
    {
      setLayout(new FormLayout());
      int buttonStyle = SelectDriverDialog.this.selectionType.getButtonType();
      
      for (int i = 0; i < SelectDriverDialog.this.m_driverOEs.length; i++) {
    	  ObjectEntry objectEntry = SelectDriverDialog.this.m_driverOEs[i];
          Button localButton = new Button(this, buttonStyle);
    	  localButton.setText(DSUtil.getNameFromOE(objectEntry));
    	  SelectDriverDialog.this.m_buttonMap.put(localButton, objectEntry);
    	  SelectDriverDialog.this.m_buttonList.add(localButton);

    	  if (i == 0)
              LayoutUtil.formLayout(localButton, null, null, 5, 5);
    	  else
    		  LayoutUtil.formLayout(localButton, null, (Control)SelectDriverDialog.this.m_buttonList.get(i - 1), 5, 5);
      }
    }
  }
}