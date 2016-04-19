package com.example.TwitterSearchTutorial;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Demonstrates how to use a twitter application keys to search
 */
public class MainActivity extends ListActivity {

	private ListActivity activity;
	final static String SearchTerm = "rats ship";
	final static String LOG_TAG = "rnc";
	String Key = null;
	String Secret = null;
	String imgUrl="http://abs.twimg.com//images//themes//theme7//bg.gif";
	ArrayList<String> headlist=new ArrayList<String>();
	ArrayList<String> timelist=new ArrayList<String>();
	ArrayList<String> desclist=new ArrayList<String>();
	ArrayList<String> imageslist=new ArrayList<String>();
	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("oncreate");
		activity = this;
		Key = getStringFromManifest("CONSUMER_KEY");
		Secret = getStringFromManifest("CONSUMER_SECRET");
		downloadSearches();
	}

	private String getStringFromManifest(String key) {
		String results = null;

		try {
			Context context = this.getBaseContext();
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			results = (String)ai.metaData.get(key);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return results;
	}

	// download twitter searches after first checking to see if there is a network connection
	public void downloadSearches() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

		if (networkInfo != null && networkInfo.isConnected()) {
			new DownloadTwitterTask().execute(SearchTerm);
		} else {
			Log.v(LOG_TAG, "No network connection available.");
		}
	}

	// Uses an AsyncTask to download data from Twitter
	private class DownloadTwitterTask extends AsyncTask<String, Void, String> {
		final static String TwitterTokenURL = "https://api.twitter.com/oauth2/token";
		final static String TwitterSearchURL = "https://api.twitter.com/1.1/search/tweets.json?q=";

		@Override
		protected String doInBackground(String... searchTerms) {
			String result = null;

			if (searchTerms.length > 0) {
				result = getSearchStream("TRS Party");
//				result = getSearchStream("Telangana");
			}
			return result;
		}

		// onPostExecute convert the JSON results into a Twitter object (which is an Array list of tweets
		@Override
		protected void onPostExecute(String result) {
			Searches searches = jsonToSearches(result);

			// lets write the results to the console as well
			for (Search search : searches) {
				Log.i(LOG_TAG, search.getText());
			}

			// send the tweets to the adapter for rendering
			
			System.out.println("result"+result);
			System.out.println("search"+searches);
			
			try {
				JSONObject objec=new JSONObject(result);
				JSONArray array=objec.optJSONArray("statuses");
				
				System.out.println("arraylength"+array.length());
				for(int i=0;i<array.length();i++)
				{
				JSONObject obj= array.getJSONObject(i);
					
				String str1=obj.getString("text");
//				headlist.add(str1);
				
				String str=obj.getString("created_at");
				
				timelist.add(str);
//				String strr=new MainActivity().getTime(str);
				
//				timelist.add(strr);
				
//				String str3=obj.getString("source");
//				desclist.add(str3);
				
				JSONObject userobj=obj.getJSONObject("user");
				
				String str4=userobj.getString("name");
				headlist.add(str4);
				
				String str5=userobj.getString("profile_image_url_https");
				imageslist.add(str5);

				String str6=userobj.getString("description");
				desclist.add(str6);
//				String str4=obj.getString("profile_image_url_https");
//				imageslist.add(str4);
				}
				System.out.println("imageurl"+imageslist.size());
				CustomList adapter=new CustomList(MainActivity.this, headlist, imageslist, timelist,desclist);
				setListAdapter(adapter);
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			ArrayAdapter<Search> adapter = new ArrayAdapter<Search>(activity, android.R.layout.simple_list_item_1, searches);
//			setListAdapter(adapter);
		}

		// converts a string of JSON data into a SearchResults object
		private Searches jsonToSearches(String result) {
			Searches searches = null;
			if (result != null && result.length() > 0) {
				try {
					Gson gson = new Gson();
					// bring back the entire search object
					SearchResults sr = gson.fromJson(result, SearchResults.class);
					// but only pass the list of tweets found (called statuses)
					searches = sr.getStatuses();
				} catch (IllegalStateException ex) {
					// just eat the exception for now, but you'll need to add some handling here
				}
			}
			return searches;
		}

		// convert a JSON authentication object into an Authenticated object
		private Authenticated jsonToAuthenticated(String rawAuthorization) {
			Authenticated auth = null;
			if (rawAuthorization != null && rawAuthorization.length() > 0) {
				try {
					Gson gson = new Gson();
					auth = gson.fromJson(rawAuthorization, Authenticated.class);
				} catch (IllegalStateException ex) {
					// just eat the exception for now, but you'll need to add some handling here
				}
			}
			return auth;
		}

		private String getResponseBody(HttpRequestBase request) {
			StringBuilder sb = new StringBuilder();
			try {

				DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
				HttpResponse response = httpClient.execute(request);
				int statusCode = response.getStatusLine().getStatusCode();
				String reason = response.getStatusLine().getReasonPhrase();

				if (statusCode == 200) {

					HttpEntity entity = response.getEntity();
					InputStream inputStream = entity.getContent();

					BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
					String line = null;
					while ((line = bReader.readLine()) != null) {
						sb.append(line);
					}
				} else {
					sb.append(reason);
				}
			} catch (UnsupportedEncodingException ex) {
			} catch (ClientProtocolException ex1) {
			} catch (IOException ex2) {
			}
			return sb.toString();
		}

		private String getStream(String url) {
			String results = null;

			// Step 1: Encode consumer key and secret
			try {
				// URL encode the consumer key and secret
				String urlApiKey = URLEncoder.encode(Key, "UTF-8");
				String urlApiSecret = URLEncoder.encode(Secret, "UTF-8");

				// Concatenate the encoded consumer key, a colon character, and the encoded consumer secret
				String combined = urlApiKey + ":" + urlApiSecret;

				// Base64 encode the string
				String base64Encoded = Base64.encodeToString(combined.getBytes(), Base64.NO_WRAP);

				// Step 2: Obtain a bearer token
				HttpPost httpPost = new HttpPost(TwitterTokenURL);
				httpPost.setHeader("Authorization", "Basic " + base64Encoded);
				httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
				httpPost.setEntity(new StringEntity("grant_type=client_credentials"));
				String rawAuthorization = getResponseBody(httpPost);
				Authenticated auth = jsonToAuthenticated(rawAuthorization);

				// Applications should verify that the value associated with the
				// token_type key of the returned object is bearer
				if (auth != null && auth.token_type.equals("bearer")) {

					// Step 3: Authenticate API requests with bearer token
					HttpGet httpGet = new HttpGet(url);

					// construct a normal HTTPS request and include an Authorization
					// header with the value of Bearer <>
					httpGet.setHeader("Authorization", "Bearer " + auth.access_token);
					httpGet.setHeader("Content-Type", "application/json");
					// update the results with the body of the response
					results = getResponseBody(httpGet);
				}
			} catch (UnsupportedEncodingException ex) {
			} catch (IllegalStateException ex1) {
			}
			return results;
		}

		private String getSearchStream(String searchTerm) {
			String results = null;
			try {
				String encodedUrl = URLEncoder.encode(searchTerm, "UTF-8");
				System.out.println("encodedUrl"+encodedUrl);
				results = getStream(TwitterSearchURL + encodedUrl + "&count=10" );
			} catch (UnsupportedEncodingException ex) {
			} catch (IllegalStateException ex1) {
			}
			return results;
		}
	}
	public class CustomList extends ArrayAdapter<String> {

		private final Activity context;
		private final ArrayList<String> time;
		private final ArrayList<String> image;
		private final ArrayList<String> head;
		private final ArrayList<String> desc;
		
	
		
		public CustomList(Activity context,ArrayList<String> time,ArrayList<String> image,ArrayList<String> head,ArrayList<String> desc)
		{
			super(context, R.layout.custom_row, head);
			
			this.context=context;
			this.time=time;
			this.image=image;
			this.head=head;
			this.desc=desc;
			
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			LayoutInflater inflater=context.getLayoutInflater();
			View rowView=inflater.inflate(R.layout.custom_row, null, true);
			
			TextView txt=(TextView)rowView.findViewById(R.id.txt_head);
			ImageView img=(ImageView)rowView.findViewById(R.id.img_user);
			TextView txt2=(TextView)rowView.findViewById(R.id.txt_time);
			TextView txt3=(TextView)rowView.findViewById(R.id.txt_desc);
			
			String stt=new MainActivity().getTime(time.get(position).toString());
			txt.setText(time.get(position));
//			txt.setText(stt);
//			img.setImageResource(image[position]);
			txt2.setText(head.get(position));
			txt3.setText(desc.get(position));
			
			
			Picasso.with(context)
		     .load(image.get(position).toString())        
		     .error(R.drawable.ic_launcher)      // optional        
		     .into(img);
			
			return rowView;
		}
	}
	
	public String getTime(String Vertrektijd){
//        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		final SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd'T'HH:mm:ss.SSSZ");
        Date dateObj;
        String newDateStr = null;
        try
        {
            dateObj = df.parse(Vertrektijd);
            SimpleDateFormat fd = new SimpleDateFormat("HH:mm");
            newDateStr = fd.format(dateObj);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
        return newDateStr;
    }
}