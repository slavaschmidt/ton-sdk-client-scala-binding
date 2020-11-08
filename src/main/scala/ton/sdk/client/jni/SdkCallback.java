package ton.sdk.client.jni;

import ton.sdk.client.modules.Api;

public interface SdkCallback<S> {
    void onSuccess(boolean finished, long responseType, S input);

    void onFailure(boolean finished, Api.SdkClientError failure);
}
