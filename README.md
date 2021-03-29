# MyImageLoader

使用方法


MyImageLoader.isWriteToLocal() 若需将图片保存到本地则先调用该方法 无需保存本地可不调用 默认不保存到本地


MyImageLoader.with(activity) 传入this/XXActivity.this


MyImageLoader.load(picUrl) 传入字符串图片链接


MyImageLoader.into(view) 传入展示图片的View


具体使用如下


需保存到本地:
MyImageLoader.isWriteToLocal().with(this).load("https://profile.csdnimg.cn/1/F/9/1_m0_52051799").into(imageView)


无需保存到本地:
MyImageLoader.with(this).load("https://profile.csdnimg.cn/1/F/9/1_m0_52051799").into(imageView)
