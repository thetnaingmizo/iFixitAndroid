package com.dozuki.ifixit.ui.guide.create;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import com.dozuki.ifixit.MainApplication;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.guide.Guide;
import com.dozuki.ifixit.model.guide.GuideStep;
import com.dozuki.ifixit.model.guide.StepLine;
import com.dozuki.ifixit.model.guide.wizard.AbstractWizardModel;
import com.dozuki.ifixit.model.guide.wizard.ModelCallbacks;
import com.dozuki.ifixit.model.guide.wizard.Page;
import com.dozuki.ifixit.ui.IfixitActivity;
import com.dozuki.ifixit.ui.guide.create.wizard.PageFragmentCallbacks;
import com.dozuki.ifixit.ui.guide.create.wizard.ReviewFragment;
import com.dozuki.ifixit.ui.guide.create.wizard.StepPagerStrip;
import com.dozuki.ifixit.util.APIError;
import com.dozuki.ifixit.util.APIEvent;
import com.dozuki.ifixit.util.APIService;
import com.squareup.otto.Subscribe;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Dialog;
import org.holoeverywhere.app.DialogFragment;
import org.holoeverywhere.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class GuideIntroActivity extends IfixitActivity implements PageFragmentCallbacks, ReviewFragment.Callbacks,
 ModelCallbacks {
   public static final int GUIDE_STEP_EDIT_REQUEST = 1;

   private boolean EDIT_INTRO_STATE = false;

   private ViewPager mPager;
   private MyPagerAdapter mPagerAdapter;

   private boolean mEditingAfterReview;

   private AbstractWizardModel mWizardModel;

   private boolean mConsumePageSelectedEvent;

   private Button mNextButton;
   private Button mPrevButton;

   private GuideIntroActivity mSelf;

   private List<Page> mCurrentPageSequence;
   private StepPagerStrip mStepPagerStrip;

   private Bundle mWizardModelBundle;

   private View.OnClickListener mNextButtonClickListener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
         if (mPager.getCurrentItem() == mCurrentPageSequence.size()) {
            DialogFragment dg = new DialogFragment() {
               @Override
               public Dialog onCreateDialog(Bundle savedInstanceState) {
                  return new AlertDialog.Builder(getActivity())
                   .setMessage(R.string.save)
                   .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                         Bundle bundle = mWizardModel.save();
                         APIService.call(mSelf, APIService.getCreateGuideAPICall(bundle));
                      }
                   })
                   .setNegativeButton(android.R.string.cancel, null)
                   .create();
               }
            };
            dg.show(getSupportFragmentManager(), "place_order_dialog");
         } else {
            if (mEditingAfterReview) {
               mPager.setCurrentItem(mPagerAdapter.getCount() - 1);
            } else {
               mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            }
         }
      }
   };

   private View.OnClickListener mPrevButtonClickListener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
         mPager.setCurrentItem(mPager.getCurrentItem() - 1);
      }
   };

   private ViewPager.SimpleOnPageChangeListener mPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
         mStepPagerStrip.setCurrentPage(position);

         if (mConsumePageSelectedEvent) {
            mConsumePageSelectedEvent = false;
            return;
         }

         mEditingAfterReview = false;
         updateBottomBar();
      }
   };

   private StepPagerStrip.OnPageSelectedListener mPageSelectedListener = new StepPagerStrip.OnPageSelectedListener() {
      @Override
      public void onPageStripSelected(int position) {
         position = Math.min(mPagerAdapter.getCount() - 1, position);
         if (mPager.getCurrentItem() != position) {
            mPager.setCurrentItem(position);
         }
      }
   };

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.guide_create_intro_main);
      mSelf = this;

      Bundle extras = getIntent().getExtras();
      if (extras != null) {
         mWizardModelBundle = extras.getBundle("model");
         Log.w("GuideIntroActivity", mWizardModelBundle.toString());
         EDIT_INTRO_STATE = true;
      }

      if (MainApplication.get().getSite().mGuideTypes != null) {
         initWizard(savedInstanceState);
      } else {
         APIService.call(this, APIService.getSiteInfoAPICall());
      }
   }

   protected void initWizard(Bundle savedInstanceState) {
      mWizardModel = new GuideIntroWizardModel(this);

      if (mWizardModelBundle != null) {
         Log.w("GuideIntroActivity extras", mWizardModelBundle.toString());
         mWizardModel.load(mWizardModelBundle);
      } else if (savedInstanceState != null) {
         Log.w("GuideIntroActivity savedinstancestate", savedInstanceState.getBundle("model").toString());
         mWizardModel.load(savedInstanceState.getBundle("model"));
      }

      mWizardModel.registerListener(this);
      mCurrentPageSequence = mWizardModel.getCurrentPageSequence();

      mPager = (ViewPager) findViewById(R.id.pager);
      mStepPagerStrip = (StepPagerStrip) findViewById(R.id.strip);

      mNextButton = (Button) findViewById(R.id.next_button);
      mPrevButton = (Button) findViewById(R.id.prev_button);

      mStepPagerStrip.setOnPageSelectedListener(mPageSelectedListener);

      mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
      mPager.setAdapter(mPagerAdapter);
      mPager.setOnPageChangeListener(mPageChangeListener);

      mNextButton.setOnClickListener(mNextButtonClickListener);
      mPrevButton.setOnClickListener(mPrevButtonClickListener);

      onPageTreeChanged();
      updateBottomBar();
   }

   @Subscribe
   public void onSiteInfo(APIEvent.SiteInfo event) {
      if (!event.hasError()) {
         MainApplication.get().setSite(event.getResult());
         initWizard(null);
      } else {
         APIService.getErrorDialog(this, event.getError(),
          APIService.getSitesAPICall()).show();
      }
   }

   @Subscribe
   public void onTopicList(APIEvent.TopicList event) {
      if (!event.hasError()) {
         ArrayList<String> topics = new ArrayList<String>(event.getResult());
         MainApplication.get().setTopics(topics);
      } else {
         APIService.getErrorDialog(this, event.getError(),
          APIService.getAllTopicsAPICall()).show();
      }
   }

   @Subscribe
   public void onGuideCreated(APIEvent.CreateGuide event) {
      if (!event.hasError()) {
         Guide guide = event.getResult();

         GuideStep item = new GuideStep(StepPortalFragment.STEP_ID++);
         item.setStepNum(0);
         item.setTitle(StepPortalFragment.DEFAULT_TITLE);
         item.addLine(new StepLine());

         ArrayList<GuideStep> initialStepList = new ArrayList<GuideStep>();
         initialStepList.add(item);

         guide.setStepList(initialStepList);

         Intent intent = new Intent(this, StepsEditActivity.class);
         intent.putExtra(GuideCreateActivity.GUIDE_KEY, guide);
         intent.putExtra(StepsEditActivity.GUIDE_STEP_KEY, 0);
         startActivityForResult(intent, GUIDE_STEP_EDIT_REQUEST);

      } else {
         event.setError(APIError.getFatalError(this));
         APIService.getErrorDialog(this, event.getError(), null).show();
      }
   }

   @Override
   public void onPageTreeChanged() {
      mCurrentPageSequence = mWizardModel.getCurrentPageSequence();
      recalculateCutOffPage();
      mStepPagerStrip.setPageCount(mCurrentPageSequence.size() + 1); // + 1 = review step
      mPagerAdapter.notifyDataSetChanged();
      updateBottomBar();
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
      mWizardModel.unregisterListener(this);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putBundle("model", mWizardModel.save());
   }

   @Override
   public AbstractWizardModel onGetModel() {
      return mWizardModel;
   }

   @Override
   public void onEditScreenAfterReview(String key) {
      for (int i = mCurrentPageSequence.size() - 1; i >= 0; i--) {
         if (mCurrentPageSequence.get(i).getKey().equals(key)) {
            mConsumePageSelectedEvent = true;
            mEditingAfterReview = true;
            mPager.setCurrentItem(i);
            updateBottomBar();
            break;
         }
      }
   }

   @Override
   public boolean finishActivityIfLoggedOut() {
      return true;
   }

   @Override
   public void onPageDataChanged(Page page) {
      if (page.isRequired()) {
         if (recalculateCutOffPage()) {
            mPagerAdapter.notifyDataSetChanged();
            updateBottomBar();
         }
      }
   }

   @Override
   public Page onGetPage(String key) {
      return mWizardModel.findByKey(key);
   }

   private boolean recalculateCutOffPage() {
      // Cut off the pager adapter at first required page that isn't completed
      int cutOffPage = mCurrentPageSequence.size() + 1;
      for (int i = 0; i < mCurrentPageSequence.size(); i++) {
         Page page = mCurrentPageSequence.get(i);
         if (page.isRequired() && !page.isCompleted()) {
            cutOffPage = i;
            break;
         }
      }

      if (mPagerAdapter.getCutOffPage() != cutOffPage) {
         mPagerAdapter.setCutOffPage(cutOffPage);
         return true;
      }

      return false;
   }

   private void updateBottomBar() {
      int position = mPager.getCurrentItem();
      if (position == mCurrentPageSequence.size()) {
         mNextButton.setText(R.string.finish);
         mNextButton.setBackgroundResource(R.drawable.wizard_finish_background);
         //mNextButton.setTextAppearance(this, R.style.TextAppearanceFinish);
      } else {
         mNextButton.setText(mEditingAfterReview
          ? R.string.review
          : R.string.next);
         mNextButton.setBackgroundResource(R.drawable.wizard_selectable_item_background);
         TypedValue v = new TypedValue();
         getTheme().resolveAttribute(android.R.attr.textAppearanceMedium, v, true);
         mNextButton.setTextAppearance(this, v.resourceId);
         mNextButton.setEnabled(position != mPagerAdapter.getCutOffPage());
      }

      mPrevButton.setVisibility(position <= 0 ? View.INVISIBLE : View.VISIBLE);
   }

   public class MyPagerAdapter extends FragmentStatePagerAdapter {
      private int mCutOffPage;
      private Fragment mPrimaryItem;

      public MyPagerAdapter(FragmentManager fm) {
         super(fm);
      }

      @Override
      public Fragment getItem(int i) {
         if (i >= mCurrentPageSequence.size()) {
            return new ReviewFragment();
         }

         return mCurrentPageSequence.get(i).createFragment();
      }

      @Override
      public int getItemPosition(Object object) {
         // TODO: be smarter about this
         if (object == mPrimaryItem) {
            // Re-use the current fragment (its position never changes)
            return POSITION_UNCHANGED;
         }

         return POSITION_NONE;
      }

      @Override
      public void setPrimaryItem(ViewGroup container, int position, Object object) {
         super.setPrimaryItem(container, position, object);
         mPrimaryItem = (Fragment) object;
      }

      @Override
      public int getCount() {
         return Math.min(mCutOffPage + 1, mCurrentPageSequence.size() + 1);
      }

      public void setCutOffPage(int cutOffPage) {
         if (cutOffPage < 0) {
            cutOffPage = Integer.MAX_VALUE;
         }
         mCutOffPage = cutOffPage;
      }

      public int getCutOffPage() {
         return mCutOffPage;
      }
   }
}