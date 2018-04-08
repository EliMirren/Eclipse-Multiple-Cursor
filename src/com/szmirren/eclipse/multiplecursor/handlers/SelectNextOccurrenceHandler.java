package com.szmirren.eclipse.multiplecursor.handlers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandlerWithState;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import com.szmirren.eclipse.multiplecursor.common.CoordinatesUtil;
import com.szmirren.eclipse.multiplecursor.common.DeleteBlockingExitPolicy;
import com.szmirren.eclipse.multiplecursor.common.ISourceViewerFinder;
import com.szmirren.eclipse.multiplecursor.common.TextUtil;

/**
 * <a href="https://github.com/caspark/eclipse-multicursor"> Update from
 * caspark</a>
 */
public class SelectNextOccurrenceHandler extends AbstractHandlerWithState {
	private static final String ID_SELECTS_IN_PROGRESS = "SELECTS_IN_PROGRESS";

	private static final class SelectInProgress {
		public final String searchText;
		public final Set<IRegion> existingSelections;
		public final int nextOffset;
		private final Point startingSelection;

		public SelectInProgress(Point startingSelection, String selectedText, Set<IRegion> existingSelections, int nextOffset) {
			this.startingSelection = startingSelection;
			this.searchText = selectedText;
			this.existingSelections = existingSelections;
			this.nextOffset = nextOffset;
		}

		@Override
		public String toString() {
			return "[Find " + searchText + " at " + nextOffset + "; original=" + startingSelection + "]";
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);
		ISourceViewer viewer = ISourceViewerFinder.fromEditorPart(editor);
		if (viewer != null) {
			startEditing(viewer);
		}

		return null;
	}

	private void startEditing(ISourceViewer viewer) throws ExecutionException {
		final Point selectedRange = viewer.getSelectedRange();
		final IDocument document = viewer.getDocument();
		try {
			final String searchText;
			final int candidateSearchOffset;
			final int selStart = CoordinatesUtil.fromOffsetAndLengthToStartAndEnd(selectedRange).x;
			if (selectedRange.y == 0) { // no characters selected
				final String documentText = document.get();
				final Point wordOffsetAndLen = TextUtil.findWordSurrounding(documentText, selStart);
				if (wordOffsetAndLen != null) {
					searchText = document.get(wordOffsetAndLen.x, wordOffsetAndLen.y);
					candidateSearchOffset = wordOffsetAndLen.x;
				} else {
					final IRegion selectedLine = document.getLineInformationOfOffset(selStart);
					searchText = document.get(selectedLine.getOffset(), selectedLine.getLength());
					candidateSearchOffset = selectedLine.getOffset();
				}
			} else {
				searchText = document.get(selectedRange.x, selectedRange.y);
				candidateSearchOffset = selectedRange.x;
			}
			SelectInProgress currentState = getCurrentState();
			final Set<IRegion> selections;
			if (LinkedModeModel.getModel(document, 0) != null && currentState != null && selectedRange.equals(currentState.startingSelection) && searchText.equals(currentState.searchText)) {
				selections = currentState.existingSelections;
				IRegion matchingRegion = findReplaceAdaptor(document, currentState.nextOffset, searchText);
				if (matchingRegion != null && !selections.contains(matchingRegion)) {
					selections.add(matchingRegion);
					saveCurrentState(new SelectInProgress(selectedRange, searchText, selections, matchingRegion.getOffset() + matchingRegion.getLength()));
					if (selections.size() == 1) {
						IRegion nextRegion = findReplaceAdaptor(document, matchingRegion.getOffset() + matchingRegion.getLength(), searchText);
						if (nextRegion != null && !selections.contains(nextRegion)) {
							selections.add(nextRegion);
							saveCurrentState(new SelectInProgress(selectedRange, searchText, selections, nextRegion.getOffset() + nextRegion.getLength()));
						}
					}
				} else {
					IRegion againMatchingRegion = findReplaceAdaptor(document, 0, searchText);
					if (againMatchingRegion != null && !selections.contains(matchingRegion)) {
						selections.add(againMatchingRegion);
						saveCurrentState(new SelectInProgress(selectedRange, searchText, selections, againMatchingRegion.getOffset() + againMatchingRegion.getLength()));
					}
				}
			} else {
				selections = new HashSet<IRegion>();
				IRegion matchingRegion = findReplaceAdaptor(document, candidateSearchOffset, searchText);
				if (matchingRegion != null && !selections.contains(matchingRegion)) {
					selections.add(matchingRegion);
					saveCurrentState(new SelectInProgress(selectedRange, searchText, selections, matchingRegion.getOffset() + matchingRegion.getLength()));
					if (selections.size() == 1) {
						IRegion nextRegion = findReplaceAdaptor(document, matchingRegion.getOffset() + matchingRegion.getLength(), searchText);
						if (nextRegion != null && !selections.contains(nextRegion)) {
							selections.add(nextRegion);
							saveCurrentState(new SelectInProgress(selectedRange, searchText, selections, nextRegion.getOffset() + nextRegion.getLength()));
						}
					}
				}
			}
			startLinkedEdit(selections, viewer, selectedRange);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 查询文档中的IRegion
	 * 
	 * @param startOffset
	 *          开始位置
	 * @param document
	 *          文档
	 * @param searchText
	 *          搜索文档
	 * @return
	 * @throws BadLocationException
	 */
	public IRegion findReplaceAdaptor(IDocument document, int startOffset, String searchText) throws BadLocationException {
		FindReplaceDocumentAdapter findReplaceAdaptor = new FindReplaceDocumentAdapter(document);
		IRegion matchingRegion = findReplaceAdaptor.find(startOffset, searchText, true, true, false, false);
		return matchingRegion;
	}

	// Reference: RenameLinkedMode class shows how linked mode is meant to be
	// used
	private void startLinkedEdit(Set<IRegion> selections, ITextViewer viewer, Point originalSelection) throws BadLocationException {
		if (selections == null || selections.isEmpty()) {
			return;
		}
		final LinkedPositionGroup linkedPositionGroup = new LinkedPositionGroup();
		for (IRegion selection : selections) {
			linkedPositionGroup.addPosition(new LinkedPosition(viewer.getDocument(), selection.getOffset(), selection.getLength()));
		}

		LinkedModeModel model = new LinkedModeModel();
		model.addGroup(linkedPositionGroup);
		model.forceInstall();
		// FIXME can add a listener here to listen for the end of linked mode
		// model.addLinkingListener(null);

		LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
		ui.setExitPolicy(new DeleteBlockingExitPolicy(viewer.getDocument()));
		ui.enter();

		// by default the text being edited is selected so restore original
		// selection
		viewer.setSelectedRange(originalSelection.x, originalSelection.y);
	}

	private SelectInProgress getCurrentState() {
		State state = getState(ID_SELECTS_IN_PROGRESS);
		if (state == null) {
			return null;
		} else {
			return (SelectInProgress) state.getValue();
		}
	}

	private void saveCurrentState(SelectInProgress selectInProgress) {
		State state = new State();
		state.setValue(selectInProgress);
		state.setId(ID_SELECTS_IN_PROGRESS);
		addState(ID_SELECTS_IN_PROGRESS, state);
	}

	@Override
	public void handleStateChange(State state, Object oldValue) {
		// logger.debug("State changed; new value=" + state.getId() + ":" +
		// state.getValue() + " and old value="
		// + oldValue);
	}

}
