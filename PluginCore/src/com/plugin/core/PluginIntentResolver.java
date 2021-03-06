package com.plugin.core;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;

import com.plugin.core.proxy.PluginProxyService;
import com.plugin.core.ui.stub.PluginStubActivity;
import com.plugin.core.ui.stub.PluginStubReceiver;
import com.plugin.util.LogUtil;
import com.plugin.util.RefInvoker;

public class PluginIntentResolver {
	private static final String RECEIVER_ID_IN_PLUGIN = "PluginDispatcher.receiver";

	/* package */static void resolveService(Intent service) {
		String targetClassName = PluginLoader.isMatchPlugin(service);
		if (targetClassName != null) {
			service.setClass(PluginLoader.getApplicatoin(), PluginProxyService.class);
			service.putExtra(PluginProxyService.SERVICE_NAME, targetClassName);
		}
	}

	/* package */static Intent resolveReceiver(final Intent intent) {
		Intent realIntent = intent;
		if (hackClassLoadForReceiverIfNeeded(intent)) {
			realIntent = new Intent();
			realIntent.setClass(PluginLoader.getApplicatoin(), PluginStubReceiver.class);
			realIntent.putExtra(RECEIVER_ID_IN_PLUGIN, intent);
		}
		return realIntent;
	}

	/* package */static void hackReceiverForClassLoader(Object msgObj) {
		Intent intent = (Intent) RefInvoker.getFieldObject(msgObj, "android.app.ActivityThread$ReceiverData", "intent");
		if (intent.getComponent().getClassName().equals(PluginStubReceiver.class.getName())) {
			Intent realIntent = (Intent) (intent.getParcelableExtra(RECEIVER_ID_IN_PLUGIN));
			LogUtil.d("receiver", realIntent.toUri(0));
			intent.putExtras(realIntent.getExtras());
			String realClassName = PluginLoader.isMatchPlugin(realIntent);
			// PluginReceiverClassLoader检测到这个特殊标记后会进行替换
			intent.setComponent(new ComponentName(intent.getComponent().getPackageName(), PluginStubReceiver.class
					.getName() + "." + realClassName));
		}
	}

	/* package */static boolean resolveStopService(final Intent name) {
		if (PluginLoader.isMatchPlugin(name) != null) {
			resolveService(name);
			name.putExtra(PluginProxyService.DESTORY_SERVICE, true);
			return true;
		}
		return false;
	}

	private static boolean hackClassLoadForReceiverIfNeeded(Intent intent) {
		// 如果在插件中发现了匹配intent的receiver项目，替换掉ClassLoader
		// 不需要在这里记录目标className，className将在Intent中传递
		if (PluginLoader.isMatchPlugin(intent) != null) {
			Object mLoadedApk = RefInvoker.getFieldObject(PluginLoader.getApplicatoin(), Application.class.getName(),
					"mLoadedApk");
			ClassLoader originalLoader = (ClassLoader) RefInvoker.getFieldObject(mLoadedApk, "android.app.LoadedApk",
					"mClassLoader");
			if (!(originalLoader instanceof PluginReceiverClassLoader)) {
				PluginReceiverClassLoader newLoader = new PluginReceiverClassLoader("", PluginLoader.getApplicatoin()
						.getCacheDir().getAbsolutePath(),
						PluginLoader.getApplicatoin().getCacheDir().getAbsolutePath(), originalLoader);
				RefInvoker.setFieldObject(mLoadedApk, "android.app.LoadedApk", "mClassLoader", newLoader);
			}
			return true;
		}
		return false;
	}

	/* package */static void resloveActivity(Intent intent) {
		// 如果在插件中发现Intent的匹配项，记下匹配的插件Activity的ClassName
		String className = PluginLoader.isMatchPlugin(intent);
		if (className != null) {
			intent.setComponent(new ComponentName(PluginLoader.getApplicatoin().getPackageName(),
					PluginStubActivity.class.getName()));
			intent.putExtra(PluginInstrumentionWrapper.ACTIVITY_NAME_IN_PLUGIN, className);
		}
	}

	static void resloveActivity(Intent[] intent) {
		// not needed
	}
}
