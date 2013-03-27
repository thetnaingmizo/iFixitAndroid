package com.dozuki.ifixit.ui.guide_create;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.guide.GuideStep;
import com.dozuki.ifixit.util.APIImage;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Fragment;

public class GuideCreateStepEditFragment extends Fragment {
  
   public interface GuideStepChangedListener {
      public void onGuideStepChanged(); 
      public void disableSave();
      public void enableSave();    
   }
   
   private static final String GUIDE_STEP_KEY = "GUIDE_STEP_KEY";
   private GuideStep mStepObject;
   private GuideCreateEditBulletFragment mEditBulletFrag;
   private GuideCreateEditMediaFragment mEditMediaFrag;  
   
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, 
       Bundle savedInstanceState) {

      View v = inflater.inflate(R.layout.guide_create_step_edit_body_new, container, false);
      Bundle b = getArguments();
      mStepObject = (GuideStep) b.getSerializable(GuideCreateStepsEditActivity.GUIDE_STEP_KEY);
      if (savedInstanceState != null) {
         mEditMediaFrag = (GuideCreateEditMediaFragment) getChildFragmentManager().findFragmentById(R.id.guide_create_edit_media_fragment_container);
         mEditBulletFrag = (GuideCreateEditBulletFragment) getChildFragmentManager().findFragmentById(R.id.guide_create_edit_bullet_fragment_container);
         
         mStepObject =  (GuideStep) savedInstanceState.getSerializable(GUIDE_STEP_KEY);
         mStepObject.getLines();
          return v;
      }

      mEditMediaFrag = new GuideCreateEditMediaFragment();

      getChildFragmentManager().beginTransaction()
              .add(R.id.guide_create_edit_media_fragment_container, mEditMediaFrag).commit(); 
      
    
      mEditBulletFrag = new GuideCreateEditBulletFragment();
      mEditBulletFrag.setRetainInstance(true);
      getChildFragmentManager().beginTransaction()
              .add(R.id.guide_create_edit_bullet_fragment_container, mEditBulletFrag).commit();
      
       setCopiesForEdit();
       
       return v;
   }


   private void setCopiesForEdit() {
      mEditBulletFrag.setSteps(mStepObject.getLines());
      mEditMediaFrag.setStepTitle(mStepObject.getTitle());
      mEditMediaFrag.setStepNumber(mStepObject.getStepNum());

      if (mStepObject.getImages().size() > 0) {

         mEditMediaFrag.setImage(GuideCreateEditMediaFragment.IMAGE_KEY_1, mStepObject.getImages().get(0));
      }

      if (mStepObject.getImages().size() > 1) {
         mEditMediaFrag.setImage(GuideCreateEditMediaFragment.IMAGE_KEY_2, mStepObject.getImages().get(1));
      }

      if (mStepObject.getImages().size() > 2) {

         mEditMediaFrag.setImage(GuideCreateEditMediaFragment.IMAGE_KEY_3, mStepObject.getImages().get(2));
      }
      
   }

   public GuideStep getGuideChanges() {
      //lines
      mStepObject.getLines().clear();
      mStepObject.getLines().addAll(mEditBulletFrag.getLines());
      //title
      mStepObject.setTitle(mEditMediaFrag.getTitle());
      //media
      //mStepObject.setImages(mEditMediaFrag.getImageIDs());
      mStepObject.getImages().clear();
      for(APIImage si : mEditMediaFrag.getImageIDs())
      {  
         if(si.mId != GuideCreateEditMediaFragment.NO_IMAGE) {
              mStepObject.addImage(si);
         }
         
      }
      return mStepObject;
   }
   
   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
    
      savedInstanceState.putSerializable(GUIDE_STEP_KEY,
         mStepObject);
   }

   public void setMediaResult(int requestCode, int resultCode, Intent data) {
      mEditMediaFrag.onActivityResult(requestCode, resultCode, data);
   }

   public GuideStep getStepObject() {
      return mStepObject;
   }

   public void setGuideStep(GuideStep guideCreateStepObject) {
      mStepObject = guideCreateStepObject;
      setCopiesForEdit();
   }  

}
