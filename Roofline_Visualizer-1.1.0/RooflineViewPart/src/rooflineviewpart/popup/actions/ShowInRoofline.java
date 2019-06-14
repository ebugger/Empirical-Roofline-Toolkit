package rooflineviewpart.popup.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.IFunctionDeclaration;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import rooflineviewpart.views.RooflineChart;
import rooflineviewpart.views.RooflineViewPart;

public class ShowInRoofline implements IObjectActionDelegate {

	private Shell shell;
	private IStructuredSelection selection;
	
	/**
	 * Constructor for Action1.
	 */
	public ShowInRoofline() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		
		
		RooflineViewPart rlView=null;
		
		try {
			IViewPart theView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("rooflineviewpart.views.RooflineViewPart");
			if(theView !=null && theView instanceof RooflineViewPart)
			{
				rlView=(RooflineViewPart)theView;
			}
			else{
				MessageDialog.openInformation(
						shell,
						"RooflineViewPart",
						"Could not access roofline view.");
				return;
			}
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	
		
		if(selection==null){
			MessageDialog.openInformation(
					shell,
					"RooflineViewPart",
					"Invalid selection.");
			return;
		}
		
		RooflineChart rc = rlView.getRooflineChart();
		
		@SuppressWarnings("unchecked")
		Iterator<Object> selit = selection.iterator();
		List<String> funcs=new ArrayList<String>();
		while(selit.hasNext()){
			ICElement cbit = (ICElement) selit.next();
			int type = cbit.getElementType();
			if (type == ICElement.C_FUNCTION||type==ICElement.C_TEMPLATE_FUNCTION)
			{
				String fullsig="";
				try {
				fullsig+=((IFunctionDeclaration) cbit).getReturnType()+" ";
				fullsig+=((IFunctionDeclaration) cbit).getSignature();
				}catch(final CModelException e) {
					e.printStackTrace();
				}
				
				//String fullsig = getFullSigniture((IFunctionDeclaration) cbit);
				funcs.add(fullsig);
			}
		}
		rc.setFunctions(funcs);
	}
	
	
	public static String getFullSigniture(IFunctionDeclaration fun)
	{
		final String returntype = fixStars(fun.getReturnType());
		String signature;
		try {
			signature = fixStars(fun.getSignature());
			return returntype + " " + signature + "#";
		} catch (final CModelException e) {
			e.printStackTrace();
		}
		return "";

	}
	
	public static String fixStars(String signature) {
		int star = signature.indexOf('*');

		while (star >= 1) {
			if (signature.charAt(star - 1) != '*')
			{
				signature = signature.substring(0, star) + " " + signature.substring(star);
				star++;
			}
			star = signature.indexOf('*', star + 1);
		}
		return signature;
	}
	

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		
		if (selection instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) selection;
		} else
		{ // if the selection is invalid, stop
			this.selection = null;
			MessageDialog.openInformation(
					shell,
					"RooflineViewPart",
					"Invalid selection.");
			return;
		}
		
	}

}
