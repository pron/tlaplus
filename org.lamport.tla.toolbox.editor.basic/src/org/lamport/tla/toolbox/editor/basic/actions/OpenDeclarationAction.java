package org.lamport.tla.toolbox.editor.basic.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.part.FileEditorInput;
import org.lamport.tla.toolbox.editor.basic.TLAEditor;
import org.lamport.tla.toolbox.editor.basic.TLAEditorActivator;
import org.lamport.tla.toolbox.editor.basic.TLAEditorAndPDFViewer;
import org.lamport.tla.toolbox.editor.basic.util.EditorUtil;
import org.lamport.tla.toolbox.spec.Spec;
import org.lamport.tla.toolbox.tool.ToolboxHandle;
import org.lamport.tla.toolbox.util.UIHelper;

/**
 * Action-based hyperlink implementation 
 * 
 * An OpenDeclarationAction object is created in  
 * TLAHyperlinkDetector.detectHyperlinks.  Its run method is called
 * to jump to the location of a definition or declaration, which is 
 * in the location field.  It is used in the OpenDeclaration (F3)
 * command.  See the commands in TLAEditor for the OpenDeclarationHandler 
 * nested class.
 * 
 * @author Simon Zambrovski
 * @version $Id$
 */
public class OpenDeclarationAction extends Action implements IHyperlink
{
    private IResource resource;
    private IRegion location;
    private String label;
    private final IRegion hyperlinkLocation;

    /**
     * Constructs the action
     * @param targetResource resource to link
     * @param targetLocation location in the resource
     * @param hyperlinkLabel label of the hyperlink
     * @param hyperlinkLocation location of the hyperlink
     */
    public OpenDeclarationAction(IResource targetResource, IRegion targetLocation, String hyperlinkLabel,
            IRegion hyperlinkLocation)
    {
        super();
        this.resource = targetResource;
        this.location = targetLocation;
        this.label = hyperlinkLabel;
        this.hyperlinkLocation = hyperlinkLocation;
    }

    /**
     * Action method  
     */
    public void run()
    {
		TLAEditorActivator
				.getDefault()
				.getLog()
				.log(new Status(IStatus.INFO, TLAEditorActivator.PLUGIN_ID,
						"Opening " + label + "(" + resource.getName() + " at "
								+ location + ")"));
        EditorUtil.setReturnFromOpenDecl();
//        // Find current location and store as a property of the spec for the
//        // Return from Goto Declaration command.
//        TLAEditor srcEditor = EditorUtil.getTLAEditorWithFocus();
//        if (srcEditor != null)
//        {
//            Spec spec = ToolboxHandle.getCurrentSpec();
//            spec.setOpenDeclModuleName(srcEditor.getEditorInput().getName());
//            spec.setOpenDeclSelection((ITextSelection) srcEditor.getSelectionProvider().getSelection());
//        }

        TLAEditor editor = ((TLAEditorAndPDFViewer) UIHelper.openEditor(TLAEditorAndPDFViewer.ID,
                new FileEditorInput((IFile) resource))).getTLAEditor();
        IRegion location1 = editor.convertRegion(false, location);
        editor.selectAndReveal(location1.getOffset(), location1.getLength());
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
     */
    public IRegion getHyperlinkRegion()
    {
        return this.hyperlinkLocation;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
     */
    public String getHyperlinkText()
    {
        return this.label;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
     */
    public String getTypeLabel()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
     */
    public void open()
    {
        run();
    }

}
