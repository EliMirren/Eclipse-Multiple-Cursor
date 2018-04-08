package com.szmirren.eclipse.multiplecursor.handlers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandlerWithState;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import com.szmirren.eclipse.multiplecursor.common.DeleteBlockingExitPolicy;
import com.szmirren.eclipse.multiplecursor.common.ISourceViewerFinder;

/**
 * 选中多个坐标输入
 * 
 * @author <a href="http://szmirren.com">Mirren</a>
 *
 */
public class MultipleCursorHandler extends AbstractHandlerWithState {
	private static final String PERSPECTIVE_LISTENER_FLAG = "perspectiveListenerFlag";
	/** 存放用户记录的位置 */
	private Set<LinkedPosition> positions = new HashSet<LinkedPosition>();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);
		ISourceViewer sourceViewer = ISourceViewerFinder.fromEditorPart(editor);
		Point point = sourceViewer.getSelectedRange();
		IDocument document = sourceViewer.getDocument();
		if (getListenerFlag() == null) {
			addState(PERSPECTIVE_LISTENER_FLAG, new State());
			HandlerUtil.getActiveWorkbenchWindowChecked(event).addPerspectiveListener(new IPerspectiveListener() {
				@Override
				public void perspectiveChanged(IWorkbenchPage arg0, IPerspectiveDescriptor arg1, String arg2) {
					positions.clear();
				}

				@Override
				public void perspectiveActivated(IWorkbenchPage arg0, IPerspectiveDescriptor arg1) {
					positions.clear();
				}
			});
			document.addDocumentListener(new IDocumentListener() {

				@Override
				public void documentChanged(DocumentEvent event) {
					positions.clear();
				}

				@Override
				public void documentAboutToBeChanged(DocumentEvent event) {
				}
			});
		}
		positions.add(new LinkedPosition(document, point.x, point.y));
		try {
			linkPosition(sourceViewer, positions, point);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 记录用户设置的坐标
	 * 
	 * @param viewer
	 * @param ps
	 * @param selectPoint
	 * @throws Exception
	 */
	public void linkPosition(ITextViewer viewer, Set<LinkedPosition> ps, Point selectPoint) throws Exception {
		if (ps == null) {
			return;
		}
		LinkedPositionGroup pGroup = new LinkedPositionGroup();
		for (LinkedPosition p : ps) {
			pGroup.addPosition(p);
		}

		LinkedModeModel model = new LinkedModeModel();
		model.addGroup(pGroup);
		model.forceInstall();
		LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
		ui.setExitPolicy(new DeleteBlockingExitPolicy(viewer.getDocument()));
		ui.enter();
		viewer.setSelectedRange(selectPoint.x, selectPoint.y);
	}

	/**
	 * 查看是否已经做事件了监听标记
	 * 
	 * @return
	 */
	public Object getListenerFlag() {
		return getState(PERSPECTIVE_LISTENER_FLAG);
	}

	@Override
	public void handleStateChange(State arg0, Object arg1) {
	}

}
