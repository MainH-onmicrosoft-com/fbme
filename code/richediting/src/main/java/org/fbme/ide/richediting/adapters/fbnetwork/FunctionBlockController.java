package org.fbme.ide.richediting.adapters.fbnetwork;

import jetbrains.mps.editor.runtime.TextBuilderImpl;
import jetbrains.mps.editor.runtime.style.StyleAttributes;
import jetbrains.mps.nodeEditor.MPSColors;
import jetbrains.mps.nodeEditor.cellLayout.AbstractCellLayout;
import jetbrains.mps.nodeEditor.cellLayout.CellLayout_Vertical;
import jetbrains.mps.nodeEditor.cells.EditorCell_Collection;
import jetbrains.mps.nodeEditor.cells.EditorCell_Property;
import jetbrains.mps.nodeEditor.cells.ModelAccessor;
import jetbrains.mps.openapi.editor.EditorContext;
import jetbrains.mps.openapi.editor.TextBuilder;
import jetbrains.mps.openapi.editor.cells.CellActionType;
import jetbrains.mps.openapi.editor.cells.EditorCell;
import org.fbme.ide.richediting.adapters.ecc.ECCEditors;
import org.fbme.ide.richediting.adapters.fb.FBTypeCellComponent;
import org.fbme.ide.richediting.editor.RichEditorStyleAttributes;
import org.fbme.ide.richediting.viewmodel.FunctionBlockPortView;
import org.fbme.ide.richediting.viewmodel.FunctionBlockView;
import org.fbme.ide.richediting.viewmodel.NetworkPortView;
import org.fbme.lib.common.Declaration;
import org.fbme.lib.iec61499.declarations.BasicFBTypeDeclaration;
import org.fbme.lib.iec61499.fbnetwork.EntryKind;
import org.fbme.lib.iec61499.instances.NetworkInstance;
import org.fbme.scenes.cells.EditorCell_Scene;
import org.fbme.scenes.controllers.LayoutUtil;
import org.fbme.scenes.controllers.components.ComponentController;
import org.fbme.scenes.controllers.scene.SceneLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.mps.openapi.model.SNode;

import java.awt.*;
import java.util.Objects;
import java.util.function.Function;

public class FunctionBlockController implements ComponentController<Point>, FBNetworkComponentController {

    private final EditorCell_Property myNameProperty;
    private final FBTypeCellComponent myFBCellComponent;
    private final EditorCell_Collection myCellCollection;

    private final FunctionBlockView myView;

    private final boolean myEditable;

    public FunctionBlockController(EditorContext context, final FunctionBlockView view) {
        myView = view;
        myEditable = myView.isEditable();
        SNode node = view.getAssociatedNode();
        myFBCellComponent = new FBTypeCellComponent(context, view.getType(), node, myEditable);
        myNameProperty = new EditorCell_Property(context, new ModelAccessor() {
            public String getText() {
                String name = view.getComponent().getName();
                return Objects.equals(name, "") ? null : name;
            }

            public void setText(String text) {
                view.getComponent().setName(text == null ? "" : text);
            }

            public boolean isValidText(String text) {
                return true;
            }
        }, node);
        myNameProperty.getStyle().set(StyleAttributes.TEXT_COLOR, myEditable ? MPSColors.BLACK : MPSColors.DARK_GRAY);
        myCellCollection = createRootCell(context, node);
        myCellCollection.getStyle().set(RichEditorStyleAttributes.FB, view.getComponent());
        myCellCollection.setBig(true);
        relayout();
    }

    private EditorCell_Collection createRootCell(EditorContext context, SNode node) {
        EditorCell_Collection foldedCell = new EditorCell_Collection(context, node, new AbstractCellLayout() {
            @Override
            public void doLayout(jetbrains.mps.openapi.editor.cells.EditorCell_Collection collection) {
                relayout();
            }

            @Override
            public TextBuilder doLayoutText(Iterable<EditorCell> iterable) {
                return new TextBuilderImpl();
            }
        }) {

            @Override
            public void onAdd() {
                super.onAdd();
                EditorCell_Collection parent = getParent().getParent();
                myFBCellComponent.getRootCell().setAction(CellActionType.BACKSPACE, parent.getAction(CellActionType.BACKSPACE));
            }
        };
        foldedCell.addEditorCell(myFBCellComponent.getRootCell());
        foldedCell.addEditorCell(myNameProperty);

        EditorCell_Collection foldableCell = new EditorCell_Collection(context, node, new CellLayout_Vertical()) {

            private boolean myUnfoldedCellInitialized = false;

            @Override
            protected void relayoutImpl() {
                super.relayoutImpl();
                FunctionBlockController.this.relayout();
            }

            @Override
            public void unfold() {
                if (myUnfoldedCellInitialized) {
                    // unfolded cell already initialized
                    super.unfold();
                    return;
                }
                addEditorCell(createUnfoldedCell());
                myUnfoldedCellInitialized = true;

                super.unfold();
            }

            private EditorCell_Scene createUnfoldedCell() {
                NetworkInstance networkInstance = getStyle().get(RichEditorStyleAttributes.NETWORK_INSTANCE);
                EditorCell_Scene scene = null;
                Declaration fbTypeDeclaration = myView.getComponent().getType().getDeclaration();
                if (fbTypeDeclaration instanceof BasicFBTypeDeclaration) {
                    scene = (EditorCell_Scene) ECCEditors.createEccEditor(context, node, SceneLayout.WINDOWED, networkInstance);
                } else {
                    scene = (EditorCell_Scene) FBNetworkEditors.createFBNetworkCell(context, node, SceneLayout.WINDOWED, networkInstance);
                }
                scene.setCellId(scene.getSNode().getNodeId().toString());
                scene.setWidth(500);
                scene.setHeight(500);

                return scene;
            }

            @Override
            protected boolean isUnderFolded() {
                return true;
            }
        };

        foldableCell.setFoldable(true);
        foldableCell.setFoldedCell(foldedCell);
        foldableCell.setInitiallyCollapsed(true);

        return foldableCell;
    }

    @Override
    public boolean canStartMoveAt(Point position, int x, int y) {
        return myEditable;
    }

    @NotNull
    @Override
    public Rectangle getBounds(@NotNull Point position) {
        return new Rectangle(position.x, position.y, myCellCollection.getWidth(), myCellCollection.getHeight());
    }

    @NotNull
    @Override
    public jetbrains.mps.nodeEditor.cells.EditorCell getComponentCell() {
        return myCellCollection;
    }

    @NotNull
    @Override
    public Point getPortCoordinates(@NotNull NetworkPortView fbPort, @NotNull Point position) {
        FunctionBlockPortView functonBlockPort = assertMine(fbPort);
        int index = functonBlockPort.getPosition();
        EntryKind kind = functonBlockPort.getKind();
        boolean isSource = functonBlockPort.isSource();
        int lineSize = getLineSize();

        Point coordinates;
        if (kind == EntryKind.EVENT) {
            coordinates = isSource ? myFBCellComponent.getOutputEventPortPosition(index) : myFBCellComponent.getInputEventPortPosition(index);
        } else if (kind == EntryKind.DATA) {
            coordinates = isSource ? myFBCellComponent.getOutputDataPortPosition(index) : myFBCellComponent.getInputDataPortPosition(index);
        } else if (kind == EntryKind.ADAPTER) {
            coordinates = isSource ? myFBCellComponent.getPlugPortPosition(index) : myFBCellComponent.getSocketPortPosition(index);
        } else {
            return null;
        }
        int shift = (myCellCollection.getWidth() - myFBCellComponent.getWidth()) / 2;
        coordinates.translate(position.x + shift, position.y + lineSize);
        return coordinates;
    }

    @NotNull
    @Override
    public Rectangle getPortBounds(@NotNull NetworkPortView fbPort, @NotNull Point position) {
        FunctionBlockPortView functonBlockPort = assertMine(fbPort);
        int index = functonBlockPort.getPosition();
        EntryKind kind = functonBlockPort.getKind();
        boolean isSource = functonBlockPort.isSource();
        int lineSize = getLineSize();

        Rectangle bounds;
        if (kind == EntryKind.EVENT) {
            bounds = isSource ? myFBCellComponent.getOutputEventPortBounds(index) : myFBCellComponent.getInputEventPortBounds(index);
        } else if (kind == EntryKind.DATA) {
            bounds = isSource ? myFBCellComponent.getOutputDataPortBounds(index) : myFBCellComponent.getInputDataPortBounds(index);
        } else if (kind == EntryKind.ADAPTER) {
            bounds = isSource ? myFBCellComponent.getPlugPortBounds(index) : myFBCellComponent.getSocketPortBounds(index);
        } else {
            return null;
        }
        bounds.translate(position.x, position.y + lineSize);
        return bounds;
    }

    @Override
    public boolean isSource(@NotNull NetworkPortView port) {
        FunctionBlockPortView functonBlockPort = assertMine(port);
        return functonBlockPort.isSource();
    }

    private FunctionBlockPortView assertMine(NetworkPortView port) {
        if (!Objects.equals(port.getComponent(), myView) || !(port instanceof FunctionBlockPortView)) {
            throw new IllegalArgumentException("invalid port");
        }
        return (FunctionBlockPortView) port;
    }

    @NotNull
    @Override
    public Point translateForm(Point originalPosition, int dx, int dy) {
        Point position = new Point(originalPosition);
        position.translate(dx, dy);
        return position;
    }

    @Override
    public Function<Point, Point> transformFormAt(Point originalForm, int x, int y) {
        return null;
    }

    @Override
    public void updateCellWithForm(Point position) {
        myCellCollection.moveTo(position.x, position.y);
        myCellCollection.relayout();
    }

    @Override
    public void updateCellSelection(boolean selected) {
        myNameProperty.getStyle().set(StyleAttributes.FONT_STYLE, selected ? Font.BOLD : Font.PLAIN);
    }

    @Override
    public void paintTrace(Graphics g, Point position) {
        int traceCenterX = position.x + myCellCollection.getWidth() / 2;
        myFBCellComponent.paintTrace((Graphics2D) g.create(), traceCenterX - myFBCellComponent.getWidth() / 2, position.y + getLineSize());
    }

    public void relayout() {
        EditorCell_Collection fbCell = myFBCellComponent.getRootCell();

        myNameProperty.relayout();
        fbCell.relayout();

        if (!myCellCollection.isCollapsed()) {
            fbCell.setX(myCellCollection.getX());
            fbCell.setY(myCellCollection.getY());
            fbCell.setWidth(myCellCollection.getWidth());
            fbCell.setHeight(myCellCollection.getHeight());
        }

        int width = Math.max(myNameProperty.getWidth(), fbCell.getWidth());
        int height = getLineSize() + fbCell.getHeight();

        EditorCell foldedCell = myCellCollection.getCells()[0];
        foldedCell.setWidth(width);
        foldedCell.setHeight(height);

        myNameProperty.moveTo(myCellCollection.getX() + width / 2 - myNameProperty.getWidth() / 2, myCellCollection.getY());
        fbCell.moveTo(myCellCollection.getX() + width / 2 - fbCell.getWidth() / 2, myCellCollection.getY() + getLineSize());
    }

    private int getLineSize() {
        return LayoutUtil.getLineSize(myCellCollection.getStyle());
    }
}
