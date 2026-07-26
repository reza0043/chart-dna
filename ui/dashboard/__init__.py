"""
پکیج dashboard: ترکیب Mixin های صفحه اصلی (چیدمان، تصویر/کراپ، استخراج مرجع،
اجرای تحلیل و نمایش نتایج) در یک کلاس واحد DashboardMixin.
"""
from ui.dashboard.layout import LayoutMixin
from ui.dashboard.image_crop import ImageCropMixin
from ui.dashboard.reference import ReferenceMixin
from ui.dashboard.analysis_runner import AnalysisRunnerMixin
from ui.dashboard.results import ResultsMixin


class DashboardMixin(
    LayoutMixin,
    ImageCropMixin,
    ReferenceMixin,
    AnalysisRunnerMixin,
    ResultsMixin,
):
    pass
