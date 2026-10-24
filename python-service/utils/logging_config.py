import logging
import sys
from logging.handlers import RotatingFileHandler
import os

def setup_logging(log_level=logging.INFO, log_file=None):
    """配置应用日志
    
    Args:
        log_level: 日志级别
        log_file: 日志文件路径，如果为None则只输出到控制台
    """
    # 创建日志格式
    log_format = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # 获取根日志器
    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)
    
    # 清除之前的处理器
    for handler in root_logger.handlers:
        root_logger.removeHandler(handler)
    
    # 添加控制台处理器
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(log_format)
    root_logger.addHandler(console_handler)
    
    # 如果提供了日志文件，添加文件处理器
    if log_file:
        # 确保日志目录存在
        log_dir = os.path.dirname(log_file)
        if log_dir and not os.path.exists(log_dir):
            os.makedirs(log_dir)
            
        # 创建滚动文件处理器，最大10MB，保留5个备份
        file_handler = RotatingFileHandler(
            log_file, maxBytes=10*1024*1024, backupCount=5
        )
        file_handler.setFormatter(log_format)
        root_logger.addHandler(file_handler)