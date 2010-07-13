package com.liato.bankdroid.banks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.util.Log;

import com.liato.bankdroid.Account;
import com.liato.bankdroid.Bank;
import com.liato.bankdroid.BankException;
import com.liato.bankdroid.Helpers;
import com.liato.bankdroid.LoginException;
import com.liato.bankdroid.R;
import com.liato.urllib.Urllib;

public class Coop extends Bank {
	private static final String TAG = "Coop";
	private static final String NAME = "Coop";
	private static final String NAME_SHORT = "coop";
	private static final String URL = "https://www.coop.se/mina-sidor/oversikt/";
	private static final int BANKTYPE_ID = Bank.COOP;

	private Pattern reViewState = Pattern.compile("__VIEWSTATE\"\\s+value=\"([^\"]+)\"");
	private Pattern reBalance = Pattern.compile("Disponibelt\\s*belopp:</td>[^>]*>([^<]+)<", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	public Coop(Context context) {
		super(context);
		super.TAG = TAG;
		super.NAME = NAME;
		super.NAME_SHORT = NAME_SHORT;
		super.BANKTYPE_ID = BANKTYPE_ID;
		super.URL = URL;
	}

	public Coop(String username, String password, Context context) throws BankException, LoginException {
		this(context);
		this.update(username, password);
	}

	@Override
	public void update() throws BankException, LoginException {
		super.update();
		if (username == null || password == null || username.length() == 0 || password.length() == 0) {
			throw new LoginException(res.getText(R.string.invalid_username_password).toString());
		}

		Urllib urlopen = new Urllib();
		String response = null;
		Matcher matcher;
		try {
			response = urlopen.open("https://www.coop.se/Mina-sidor/Oversikt/");
			matcher = reViewState.matcher(response);
			if (!matcher.find()) {
				throw new BankException(res.getText(R.string.unable_to_find).toString()+" viewstate.");
			}
			String strViewState = matcher.group(1);
			List <NameValuePair> postData = new ArrayList <NameValuePair>();
			postData.add(new BasicNameValuePair("ctl00$ContentPlaceHolderTodo$ContentPlaceHolderMainPageContainer$ContentPlaceHolderMainPageWithNavigationAndGlobalTeaser$ContentPlaceHolderPreContent$RegisterMediumUserForm$TextBoxUserName", username));
			postData.add(new BasicNameValuePair("ctl00$ContentPlaceHolderTodo$ContentPlaceHolderMainPageContainer$ContentPlaceHolderMainPageWithNavigationAndGlobalTeaser$ContentPlaceHolderPreContent$RegisterMediumUserForm$TextBoxPassword", password));
			postData.add(new BasicNameValuePair("ctl00$ContentPlaceHolderTodo$ContentPlaceHolderMainPageContainer$ContentPlaceHolderMainPageWithNavigationAndGlobalTeaser$ContentPlaceHolderPreContent$RegisterMediumUserForm$ButtonLogin", "Logga in"));
			postData.add(new BasicNameValuePair("__VIEWSTATE", strViewState));
			postData.add(new BasicNameValuePair("__EVENTTARGET", ""));
			postData.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
			response = urlopen.open("https://www.coop.se/Mina-sidor/Oversikt/", postData);
			Log.d(TAG, urlopen.getCurrentURI());
			if (response.contains("Felmeddelande")) {
				throw new LoginException(res.getText(R.string.invalid_username_password).toString());
			}
			/*
			for (String s : response.split("\n")) {
				Log.d(TAG, s);
			}
			*/			
			response = urlopen.open("https://www.coop.se/Mina-sidor/Oversikt/?t=MedMeraVisa");
			matcher = reBalance.matcher(response);
			while (matcher.find()) {
				accounts.add(new Account("Betalkort", Helpers.parseBalance(matcher.group(1).trim()), "1"));
				balance = balance.add(Helpers.parseBalance(matcher.group(1)));
			}
			if (accounts.isEmpty()) {
				throw new BankException(res.getText(R.string.no_accounts_found).toString());
			}
		}
		catch (ClientProtocolException e) {
			throw new BankException(e.getMessage());
		}
		catch (IOException e) {
			throw new BankException(e.getMessage());
		}
		finally {
			urlopen.close();
		}

	}
}