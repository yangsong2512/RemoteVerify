package com.omniremotes.remoteverify.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.omniremotes.remoteverify.R;
import com.omniremotes.remoteverify.fragment.TestCaseFragment;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TestCaseAdapter extends BaseAdapter {
    private static final String TAG="RemoteVerify-TestCaseAdapter";
    private Context mContext;
    private List<TestCase> mTestCaseList;
    private TestCaseFragment mTestCaseFragment;
    private String mCurrentCase = null;
    private class TestCase{
        String title;
        String desc;
        boolean success;
        int errorCode;
        TestCase(String title,String desc){
            this.title = title;
            this.desc = desc;
        }
    }
    class ViewHolder {
        TextView titleTextView;
        TextView descTextView;
        Button startButton;
        ProgressBar progressBar;
    }

    public TestCaseAdapter(Context context,TestCaseFragment testCaseFragment,String address){
        mContext = context;
        mTestCaseFragment = testCaseFragment;
        if(mTestCaseList == null){
            mTestCaseList = new ArrayList<>();
        }
    }

    public void parserTestCase(){
        try{
            InputStream inputStream = mContext.getAssets().open("Projects.xml");
            Log.d(TAG,"start xml parser");
            new TestCaseParserTask().execute(inputStream);
        }catch (IOException e){
            Log.d(TAG,""+e);
        }
    }

    private boolean parseCaseAttr(XmlPullParser parser){
        int count = parser.getAttributeCount();
        String title=null;
        String desc=null;
        for(int i = 0;i < count;i++){
            String key = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            Log.d(TAG,"key:"+key+",value:"+value);
            if(key.equals("Enable")&&value.equals("false")){
                Log.d(TAG,"Enable:"+value);
                return false;
            }
            if(key.equals("Desc")){
                desc = value;
                Log.d(TAG,"desc:"+desc);

            }
            if(key.equals("Title")){
                title = value;
                Log.d(TAG,"title:"+title);
            }
        }
        if(desc != null && title != null){
            Log.d(TAG,"add new case");
            mTestCaseList.add(new TestCase(title,desc));
        }
        return true;
    }

    private void startParser(InputStream inputStream) throws Exception{
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream,"utf-8");
        int event = parser.getEventType();
        boolean skip = false;
        while (true){
            if(event == XmlPullParser.START_DOCUMENT){
                Log.d(TAG,"START");
            }else if(event == XmlPullParser.START_TAG){
                if(skip){
                    event = parser.next();
                    continue;
                }
                String tagName = parser.getName();
                Log.d(TAG,"start tag:"+tagName);
                if(tagName!=null){
                    if(tagName.equals("Case")){
                        if(!parseCaseAttr(parser)){
                            skip = true;
                        }
                    }
                }
            }else if(event == XmlPullParser.END_TAG){
                String tagName = parser.getName();
                if(tagName.equals("Case")){
                    skip = false;
                }
                Log.d(TAG,"end tag:"+tagName);
            }else if(event == XmlPullParser.END_DOCUMENT){
                Log.d(TAG,"END");
                break;
            }
            event = parser.next();
        }
    }

    private class TestCaseParserTask extends AsyncTask<InputStream,Void,Void>{
        @Override
        protected Void doInBackground(InputStream... inputStreams) {
            InputStream inputStream = inputStreams[0];
            try{
                startParser(inputStream);
                inputStream.close();
            }catch (Exception e){
                Log.d(TAG,""+e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            notifyDataSetChanged();
        }
    }

    @Override
    public Object getItem(int position) {
        return mTestCaseList.get(position);
    }

    @Override
    public int getCount() {
        return mTestCaseList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if(convertView == null){
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.test_case_list_item,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.titleTextView = convertView.findViewById(R.id.test_case_title);
            viewHolder.descTextView = convertView.findViewById(R.id.test_case_description);
            viewHolder.progressBar = convertView.findViewById(R.id.test_case_progress);
            viewHolder.startButton = convertView.findViewById(R.id.test_case_run);
            viewHolder.startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mTestCaseFragment != null){
                        viewHolder.startButton.setVisibility(View.GONE);
                        viewHolder.progressBar.setVisibility(View.VISIBLE);
                        mCurrentCase = mTestCaseList.get(position).title;
                        mTestCaseFragment.onStartButtonClicked(mCurrentCase);
                    }
                }
            });
            convertView.setTag(viewHolder);
        }else{
            viewHolder =(ViewHolder) convertView.getTag();
        }
        TestCase testCase = mTestCaseList.get(position);
        viewHolder.titleTextView.setText(testCase.title);
        viewHolder.descTextView.setText(testCase.desc);
        if(mCurrentCase != null){
            if(mCurrentCase.equals("Pairing Test")){
                if(testCase.success){
                    viewHolder.startButton.setVisibility(View.VISIBLE);
                    viewHolder.progressBar.setVisibility(View.GONE);
                }
            }
        }
        return convertView;
    }

    public void notifyPairFailed(){

    }

    public void notifyDeviceConnected(String address){
        Log.d(TAG,"pair success:"+mCurrentCase);
        if(mCurrentCase.equals("Pairing Test")){
            for(TestCase testCase:mTestCaseList){
                if(testCase.title.equals(mCurrentCase)){
                    Log.d(TAG,"update status");
                    testCase.success=true;
                    notifyDataSetChanged();
                }
            }
        }
    }

    public void notifyPairSuccess(){

    }

    public void clearDataSet(){
        if(mTestCaseList!=null){
            mTestCaseList.clear();
        }
    }
}
