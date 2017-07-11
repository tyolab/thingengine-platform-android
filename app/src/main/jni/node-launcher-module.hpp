//
// Created by gcampagn on 7/10/17.
//

#ifndef THINGENGINE_PLATFORM_ANDROID_NODE_LAUNCHER_MODULE_HPP
#define THINGENGINE_PLATFORM_ANDROID_NODE_LAUNCHER_MODULE_HPP

#include <list>
#include <unordered_map>

#include "node-launcher-marshal.hpp"

namespace thingengine_node_launcher {

class CallQueue : private uv_async_t {
private:
    std::list<PackagedNodeCall> calls;
    std::mutex mutex;
    std::condition_variable init_cond;
    v8::Isolate *isolate;
    v8::Global<v8::Function> receiver;
    v8::Global<v8::Object> this_obj;
    bool initialized;

    static void run_all_asyncs(uv_async_t *_async);

public:
    void Init(v8::Isolate *isolate);
    void SetNodeReceiver(v8::Local<v8::Function> &fn, v8::Local<v8::Object> &this_obj);
    void InvokeAsync(std::u16string &&function_name, std::vector<InteropValue> &&arguments);
    InteropValue InvokeSync(std::u16string &&function_name, std::vector<InteropValue> &&arguments);
};

class NativeCallQueue : private uv_async_t {
private:
    std::list<std::function<void()>> calls;
    std::mutex mutex;
    std::condition_variable init_cond;
    bool initialized;

    static void run_all_asyncs(uv_async_t *_async);

public:
    void Init();

    template<class Callback>
    void InvokeAsync(Callback &&call) {
        std::unique_lock<std::mutex> lock(mutex);
        while (!initialized) init_cond.wait(lock);
        calls.emplace_back(std::forward<Callback>(call));
        uv_async_send(this);
    }
};

class LocalJavaFrame {
private:
    JNIEnv *env;

public:
    LocalJavaFrame(JNIEnv *env) : env(env) {
        env->PushLocalFrame(20);
    }

    ~LocalJavaFrame() {
        env->PopLocalFrame(nullptr);
    }

    LocalJavaFrame(const LocalJavaFrame &) = delete;

    LocalJavaFrame(LocalJavaFrame &&) = delete;

    LocalJavaFrame &operator=(const LocalJavaFrame &) = delete;

    LocalJavaFrame &operator=(LocalJavaFrame &&) = delete;
};

class JavaInvoker {
private:
    JNIEnv *env;
    jmethodID invoke;
    jobject executor;
    jmethodID executor_execute;
    jclass JavaCallback;
    jclass NodeJSLauncher;
    jmethodID NodeJSLauncher_invokeAsync;
    std::mutex method_lock;
    std::unordered_map<std::u16string, jobject> methods;
    std::unordered_map<jlong, v8::Global<v8::Promise::Resolver>> callbacks;
    jlong next_promise_id = 1;

public:
    void Init(JNIEnv *env, jobject jClassLoader);
    void Deinit();

    InteropValue InvokeSync(v8::Isolate *isolate, std::u16string &&function_name,
                            std::vector<InteropValue> &&arguments);
    v8::Local<v8::Promise> InvokeAsync(v8::Isolate *isolate, std::u16string &&function_name,
                                   std::vector<InteropValue> &&arguments);

    void CompletePromise(jlong promiseId, const v8::Maybe<std::u16string> &error_msg,
                         const InteropValue &result);

    void Register(std::u16string &&fn, jobject method) {
        std::unique_lock<std::mutex> lock(method_lock);
        methods[std::move(fn)] = method;
    }
};

struct GlobalState {
    CallQueue queue;
    NativeCallQueue native_queue;
    v8::Global<v8::Object> launcher_module;
    bool terminate;

    JavaInvoker java_invoker;
};

extern GlobalState global_state;
extern node::node_module launcher_module;

}

#endif //THINGENGINE_PLATFORM_ANDROID_NODE_LAUNCHER_MODULE_HPP
