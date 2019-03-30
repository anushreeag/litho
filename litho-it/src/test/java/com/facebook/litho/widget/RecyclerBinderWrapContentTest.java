/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.widget.RecyclerBinderTest.NO_OP_CHANGE_SET_COMPLETE_CALLBACK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import android.os.Looper;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Size;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

/** Tests for {@link RecyclerBinder} with wrap content enabled. */
@RunWith(ComponentsTestRunner.class)
public class RecyclerBinderWrapContentTest {

  private static final float RANGE_RATIO = 2.0f;

  private ComponentContext mComponentContext;
  private Looper mLayoutThreadShadowLooper;

  private TestRecyclerView mRecyclerView;

  @Before
  public void setup() throws Exception {
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
    mRecyclerView = new TestRecyclerView(mComponentContext.getAndroidContext());

    mLayoutThreadShadowLooper =
        (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper");
  }

  @Test
  public void testWrapContentWithInsertOnVertical() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .wrapContent(true)
            .build(mComponentContext);
    recyclerBinder.mount(mRecyclerView);

    final Component component =
        TestDrawableComponent.create(mComponentContext).measuredHeight(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.insertItemAt(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);
    final Size size = new Size();

    // Manually invoke the remeasure to get the measured size
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(100);

    // Keep inserting items to exceed the maximum height
    for (int i = 0; i < 10; i++) {
      final Component newComponent =
          TestDrawableComponent.create(mComponentContext).measuredHeight(100).build();
      recyclerBinder.insertItemAt(
          i + 1, ComponentRenderInfo.create().component(newComponent).build());
      recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);
    }

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(10);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(1000);
  }

  @Test
  public void testWrapContentWithInsertRangeOnVertical() {
    final int NUM_TO_INSERT = 6;
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .wrapContent(true)
            .build(mComponentContext);
    recyclerBinder.mount(mRecyclerView);

    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredHeight(100).build();
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAt(0, renderInfos);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);
    final Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(600);

    // Keep inserting items to exceed the maximum height.
    final ArrayList<RenderInfo> newRenderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredHeight(100).build();
      newRenderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAt(0, newRenderInfos);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(1000);
  }

  @Test
  public void testWrapContentWithRemoveOnVertical() {
    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.VERTICAL, 100);

    recyclerBinder.mount(mRecyclerView);

    recyclerBinder.removeItemAt(0);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(700);
  }

  @Test
  public void testWrapContentWithRemoveRangeOnVertical() {
    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.VERTICAL, 100);

    recyclerBinder.mount(mRecyclerView);

    recyclerBinder.removeRangeAt(0, 3);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(500);
  }

  @Test
  public void testWrapContentWithUpdateOnVertical() {
    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.VERTICAL, 100);

    recyclerBinder.mount(mRecyclerView);

    final Component newComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c).measuredHeight(200).build();
          }
        };
    recyclerBinder.updateItemAt(0, newComponent);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(900);
  }

  @Test
  public void testWrapContentWithUpdateRangeOnVertical() {
    final int NUM_TO_UPDATE = 5;
    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.VERTICAL, 100);

    recyclerBinder.mount(mRecyclerView);

    final ArrayList<RenderInfo> newRenderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_UPDATE; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredHeight(50).build();
      newRenderInfos.add(ComponentRenderInfo.create().component(component).build());
    }
    recyclerBinder.updateRangeAt(0, newRenderInfos);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(550);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithInsertAsyncOnVertical() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .wrapContent(true)
            .build(mComponentContext);

    recyclerBinder.mount(mRecyclerView);

    final Component component =
        TestDrawableComponent.create(mComponentContext).measuredHeight(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);
    final Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(100);

    // Keep inserting items to exceed the maximum height.
    for (int i = 0; i < 10; i++) {
      final Component newComponent =
          TestDrawableComponent.create(mComponentContext).measuredHeight(100).build();
      recyclerBinder.insertItemAtAsync(
          i + 1, ComponentRenderInfo.create().component(newComponent).build());
      recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);
    }

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isGreaterThanOrEqualTo(10);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(1000);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithInsertRangeAsyncOnVertical() {
    final int NUM_TO_INSERT = 6;
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .wrapContent(true)
            .build(mComponentContext);

    recyclerBinder.mount(mRecyclerView);

    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredHeight(100).build();
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAtAsync(0, renderInfos);
    recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);
    final Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(600);

    // Keep inserting items to exceed the maximum height.
    final ArrayList<RenderInfo> newRenderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredHeight(100).build();
      newRenderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAtAsync(0, newRenderInfos);
    recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isGreaterThanOrEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(1000);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithRemoveAsyncOnVertical() {
    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.VERTICAL, 100, true);

    recyclerBinder.mount(mRecyclerView);

    recyclerBinder.removeItemAtAsync(0);
    recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isGreaterThanOrEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(700);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithRemoveRangeAsyncOnVertical() {
    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.VERTICAL, 100, true);

    recyclerBinder.mount(mRecyclerView);

    recyclerBinder.removeRangeAtAsync(0, 3);
    recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isGreaterThanOrEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(500);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithUpdateAsyncOnVertical() {
    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.VERTICAL, 100, true);

    recyclerBinder.mount(mRecyclerView);

    final Component newComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c).measuredHeight(200).build();
          }
        };
    final RenderInfo renderInfo = ComponentRenderInfo.create().component(newComponent).build();

    recyclerBinder.updateItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isGreaterThanOrEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(900);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithUpdateRangeAsyncOnVertical() {
    final int NUM_TO_UPDATE = 5;
    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(1000, AT_MOST);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.VERTICAL, 100, true);

    recyclerBinder.mount(mRecyclerView);

    final ArrayList<RenderInfo> newRenderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_UPDATE; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredHeight(50).build();
      newRenderInfos.add(ComponentRenderInfo.create().component(component).build());
    }
    recyclerBinder.updateRangeAtAsync(0, newRenderInfos);
    recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isGreaterThanOrEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.height).isEqualTo(550);
  }

  @Test
  public void testWrapContentWithInsertOnHorizontal() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .layoutInfo(
                new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false))
            .rangeRatio(RANGE_RATIO)
            .wrapContent(true)
            .build(mComponentContext);

    recyclerBinder.mount(mRecyclerView);

    final Component component =
        TestDrawableComponent.create(mComponentContext).measuredWidth(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.insertItemAt(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);
    final Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(100);

    // Keep inserting items to exceed the maximum height.
    for (int i = 0; i < 10; i++) {
      final Component newComponent =
          TestDrawableComponent.create(mComponentContext).measuredWidth(100).build();
      recyclerBinder.insertItemAt(
          i + 1, ComponentRenderInfo.create().component(newComponent).build());
      recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);
    }

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(10);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(1000);
  }

  @Test
  public void testWrapContentWithInsertRangeOnHorizontal() {
    final int NUM_TO_INSERT = 6;
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .layoutInfo(
                new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false))
            .rangeRatio(RANGE_RATIO)
            .wrapContent(true)
            .build(mComponentContext);

    recyclerBinder.mount(mRecyclerView);

    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredWidth(100).build();
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAt(0, renderInfos);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);
    final Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(600);

    // Keep inserting items to exceed the maximum height.
    final ArrayList<RenderInfo> newRenderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredWidth(100).build();
      newRenderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAt(0, newRenderInfos);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(1000);
  }

  @Test
  public void testWrapContentWithRemoveOnHorizontal() {
    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.HORIZONTAL, 100);

    recyclerBinder.mount(mRecyclerView);

    recyclerBinder.removeItemAt(0);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(700);
  }

  @Test
  public void testWrapContentWithRemoveRangeOnHorizontal() {
    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.HORIZONTAL, 100);

    recyclerBinder.mount(mRecyclerView);

    recyclerBinder.removeRangeAt(0, 3);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(500);
  }

  @Test
  public void testWrapContentWithUpdateOnHorizontal() {
    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.HORIZONTAL, 100);

    recyclerBinder.mount(mRecyclerView);

    final Component newComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c).measuredWidth(200).build();
          }
        };
    recyclerBinder.updateItemAt(0, newComponent);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(900);
  }

  @Test
  public void testWrapContentWithUpdateRangeOnHorizontal() {
    final int NUM_TO_UPDATE = 5;
    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.HORIZONTAL, 100);

    recyclerBinder.mount(mRecyclerView);

    final ArrayList<RenderInfo> newRenderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_UPDATE; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredWidth(50).build();
      newRenderInfos.add(ComponentRenderInfo.create().component(component).build());
    }
    recyclerBinder.updateRangeAt(0, newRenderInfos);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(550);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithInsertAsyncOnHorizontal() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .layoutInfo(
                new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false))
            .rangeRatio(RANGE_RATIO)
            .wrapContent(true)
            .build(mComponentContext);

    recyclerBinder.mount(mRecyclerView);

    final Component component =
        TestDrawableComponent.create(mComponentContext).measuredWidth(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);
    final Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(100);

    // Keep inserting items to exceed the maximum height.
    for (int i = 0; i < 10; i++) {
      final Component newComponent =
          TestDrawableComponent.create(mComponentContext).measuredWidth(100).build();
      recyclerBinder.insertItemAtAsync(
          i + 1, ComponentRenderInfo.create().component(newComponent).build());
      recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);
    }

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(20);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(1000);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithInsertRangeAsyncOnHorizontal() {
    final int NUM_TO_INSERT = 6;
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .layoutInfo(
                new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false))
            .rangeRatio(RANGE_RATIO)
            .wrapContent(true)
            .build(mComponentContext);

    recyclerBinder.mount(mRecyclerView);

    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredWidth(100).build();
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAtAsync(0, renderInfos);
    recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);
    final Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(600);

    // Keep inserting items to exceed the maximum height.
    final ArrayList<RenderInfo> newRenderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredWidth(100).build();
      newRenderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAtAsync(0, newRenderInfos);
    recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(2);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(1000);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithRemoveAsyncOnHorizontal() {
    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.HORIZONTAL, 100, true);

    recyclerBinder.mount(mRecyclerView);

    recyclerBinder.removeItemAt(0);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isGreaterThanOrEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(700);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithRemoveRangeAsyncOnHorizontal() {
    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.HORIZONTAL, 100, true);

    recyclerBinder.mount(mRecyclerView);

    recyclerBinder.removeRangeAt(0, 3);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isGreaterThanOrEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(500);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithUpdateAsyncOnHorizontal() {
    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.HORIZONTAL, 100, true);

    recyclerBinder.mount(mRecyclerView);

    final Component newComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c).measuredWidth(200).build();
          }
        };
    recyclerBinder.updateItemAt(0, newComponent);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isGreaterThanOrEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(900);
  }

  // TODO(t33888191): Support wrapContent with insertAsync
  // @Test
  public void testWrapContentWithUpdateRangeAsyncOnHorizontal() {
    final int NUM_TO_UPDATE = 5;
    final int widthSpec = makeSizeSpec(1000, AT_MOST);
    final int heightSpec = makeSizeSpec(1000, EXACTLY);

    final RecyclerBinder recyclerBinder =
        prepareBinderWithMeasuredChildSize(
            widthSpec, heightSpec, 8, OrientationHelper.HORIZONTAL, 100, true);

    recyclerBinder.mount(mRecyclerView);

    final ArrayList<RenderInfo> newRenderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_UPDATE; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).measuredWidth(50).build();
      newRenderInfos.add(ComponentRenderInfo.create().component(component).build());
    }
    recyclerBinder.updateRangeAt(0, newRenderInfos);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isGreaterThanOrEqualTo(1);
    verifyListContainsAllSameInstance(
        recyclerBinder.mRemeasureRunnable, mRecyclerView.getPostAnimationRunnableList());

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(550);
  }

  @Test
  public void testOnDataRenderedWithNoChanges() {
    final LithoRecylerView recyclerView =
        new LithoRecylerView(mComponentContext.getAndroidContext());
    final ChangeSetCompleteCallback changeSetCompleteCallback =
        mock(ChangeSetCompleteCallback.class);
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .wrapContent(true)
            .build(mComponentContext);
    recyclerBinder.mount(recyclerView);

    final Component component =
        TestDrawableComponent.create(mComponentContext).measuredHeight(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.insertItemAt(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(true, changeSetCompleteCallback);

    verify(changeSetCompleteCallback).onDataRendered(eq(true), anyLong());

    reset(changeSetCompleteCallback);

    // Call notifyChangeSetComplete with no actual data change.
    recyclerBinder.notifyChangeSetComplete(false, changeSetCompleteCallback);

    verify(changeSetCompleteCallback).onDataRendered(eq(true), anyLong());
    assertThat(mRecyclerView.getPostAnimationRunnableList()).isEmpty();
  }

  private RecyclerBinder prepareBinderWithMeasuredChildSize(
      int widthSpec, int heightSpec, int count, int orientation, int childSize) {
    return prepareBinderWithMeasuredChildSize(
        widthSpec, heightSpec, count, orientation, childSize, false);
  }

  private RecyclerBinder prepareBinderWithMeasuredChildSize(
      int widthSpec,
      int heightSpec,
      int count,
      final int orientation,
      final int childSize,
      boolean async) {
    RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .layoutInfo(new LinearLayoutInfo(mComponentContext, orientation, false))
            .wrapContent(true)
            .build(mComponentContext);

    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      final Component component =
          new InlineLayoutSpec() {
            @Override
            protected Component onCreateLayout(ComponentContext c) {
              TestDrawableComponent.Builder builder = TestDrawableComponent.create(c);
              if (orientation == OrientationHelper.VERTICAL) {
                return builder.measuredHeight(childSize).build();
              } else {
                return builder.measuredWidth(childSize).build();
              }
            }
          };

      components.add(ComponentRenderInfo.create().component(component).build());
    }
    if (async) {
      recyclerBinder.insertRangeAtAsync(0, components);
      recyclerBinder.notifyChangeSetCompleteAsync(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);
      Shadows.shadowOf(mLayoutThreadShadowLooper).runToEndOfTasks();
    } else {
      recyclerBinder.insertRangeAt(0, components);
      recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);
    }

    return recyclerBinder;
  }

  private void verifyListContainsAllSameInstance(Runnable r, List<Runnable> list) {
    for (Runnable runnable : list) {
      assertThat(runnable).isSameAs(r);
    }
  }
}
